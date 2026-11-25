package com.keopsfenks.arcana.skills.platform.auras

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.execution.ScheduledWork
import com.keopsfenks.arcana.skills.platform.execution.*
import com.keopsfenks.arcana.skills.platform.managed.ManagedExecutionState
import com.keopsfenks.arcana.skills.platform.values.InvocationFrame
import java.util.Optional
import java.util.concurrent.*
import java.util.concurrent.atomic.*

class AuraInstance internal constructor(
    val definition: AuraDefinition, val managed: ManagedExecutionState,
    val options: AuraOptions, val caster: SkillActor, private val recipient: SkillActor,
    private val clock: () -> Long, private val removed: (AuraInstance) -> Unit
) : SkillManagedHandle, SkillAuraContext {
    private val phase = AtomicInteger(0) // active, ending, disposed
    private val removal = AtomicReference<SkillRemovalReason?>()
    private val stackCount = AtomicInteger(1)
    private val chargeCount = AtomicInteger(options.charges)
    private val born = clock()
    private val timerLock = Any()
    private var expiry: ScheduledWork<Unit>? = null
    private var ticking: ScheduledWork<Unit>? = null
    private var reconciliation: ScheduledWork<Unit>? = null
    private var expiryGeneration = 0L
    private var refreshAge = options.durationTicks * 50_000_000L
    private val changes = ConcurrentHashMap<java.util.UUID, CompiledSkillMechanic>()
    private val changeCounter = AtomicLong()
    val changeVersion get() = changeCounter.get()
    val started = CompletableFuture<SkillManagedHandle>()
    val finished = CompletableFuture<SkillResult>()
    val stacks get() = stackCount.get()
    val charges get() = chargeCount.get()
    override fun instance(): SkillManagedHandle = this
    override fun caster() = caster
    override fun stacks() = stacks
    override fun charges() = charges
    override fun own(cleanup: AutoCloseable) = managed.own(cleanup)
    override fun execute(hooks: List<CompiledSkillMechanic>) = managed.execute(hooks, values())
    override fun subscribeEvent(kind: SkillKey, hooks: List<CompiledSkillMechanic>, interceptors: List<CompiledSkillInterceptor>, condition: SkillPredicate, consumeCharge: Int) =
        managed.platform.auraEvents.subscribe(kind, this, hooks, interceptors, condition, consumeCharge)
    override fun addCharges(delta: Int) = addCharges(delta, null)
    override fun id() = managed.lease.id
    override fun skill() = definition.key
    override fun revision() = definition.revision.revisionId
    override fun actor() = recipient
    override fun active() = phase.get() == 0 && managed.lease.active
    override fun reason(): Optional<SkillRemovalReason> = Optional.ofNullable(removal.get())
    init {
        managed.aura = this
        managed.deadlineAction = { remove(SkillRemovalReason.EXPIRED) }
        managed.lease.onClose {
            val retired = synchronized(phase) {
                if (phase.get() == 2) false else {
                    removal.compareAndSet(null, managed.lease.reason ?: if (!definition.revision.available()) SkillRemovalReason.DEFINITION_RELOADED else SkillRemovalReason.CANCELLED)
                    phase.set(2); true
                }
            }
            if (retired) {
                cancelTimers(); removed(this)
                started.completeExceptionally(CancellationException("Aura retired during initialization"))
                finished.complete(SkillResult.of(SkillResult.Status.CANCELLED, "Aura retired"))
            }
        }
    }
    internal fun start() {
        if (!active()) return
        expiration(options.durationTicks)
        managed.execute(definition.components + definition.onStart, values()).whenComplete { result, error ->
            if (!active()) return@whenComplete
            val outcome = if (error == null) result else PlatformExecutionState.failure(error)
            if (!outcome.isSuccess) { started.completeExceptionally(SkillProviderException(outcome)); remove(failureReason(outcome)) }
            else { started.complete(this); tick() }
        }
    }
    internal fun values(): Map<String, Any> = mapOf("aura_stacks" to stacks, "aura_charges" to charges, "aura_name" to definition.key.toString(), "aura_change_version" to changeVersion)
    fun onChanged(hook: CompiledSkillMechanic): AutoCloseable {
        check(active()) { "Aura is inactive" }
        require(changes.size < 64) { "Maximum 64 aura reconciliation subscribers" }
        val id = java.util.UUID.randomUUID(); changes[id] = hook
        val subscription = AutoCloseable { changes.remove(id) }
        managed.own(subscription)
        if (!active()) subscription.close()
        return subscription
    }
    private fun changed(): CompletableFuture<SkillResult> {
        val hooks = changes.values.toList()
        return if (hooks.isEmpty() || !active()) CompletableFuture.completedFuture(SkillResult.success()) else managed.execute(hooks, values())
    }
    private fun deferChanged() {
        synchronized(timerLock) {
            if (!active() || changes.isEmpty() || reconciliation != null) return
            val work = managed.engine.scheduler.on(recipient.entity(), 1) {
                synchronized(timerLock) { reconciliation = null }
                if (active()) changed().whenComplete { result, error ->
                    if (active()) {
                        val outcome = if (error == null) result else PlatformExecutionState.failure(error)
                        if (!outcome.isSuccess) remove(failureReason(outcome))
                    }
                }
                Unit
            }
            reconciliation = work
            work.future.whenComplete { _, error -> if (error != null && active() && synchronized(timerLock) { reconciliation === work }) remove(SkillRemovalReason.ACTOR_RETIRED) }
        }
    }
    private fun expired() = clock() - born >= refreshAge || clock() - born >= MAX_NANOS
    private fun tick() {
        synchronized(timerLock) {
            if (!active() || ticking != null || definition.onTick.isEmpty()) return
            val work = managed.engine.scheduler.on(recipient.entity(), options.intervalTicks) {
                synchronized(timerLock) { ticking = null }
                if (active()) {
                    if (!InvocationSnapshots.valid(recipient.entity())) remove(SkillRemovalReason.ACTOR_RETIRED)
                    else if (expired()) remove(SkillRemovalReason.EXPIRED)
                    else managed.execute(definition.onTick, values()).whenComplete { result, error ->
                        if (active()) {
                            val outcome = if (error == null) result else PlatformExecutionState.failure(error)
                            if (outcome.isSuccess) tick() else remove(failureReason(outcome))
                        }
                    }
                }
                Unit
            }
            ticking = work
            work.future.whenComplete { _, error -> if (error != null && synchronized(timerLock) { ticking === work }) remove(SkillRemovalReason.ACTOR_RETIRED) }
        }
    }
    private fun expiration(ticks: Long) {
        synchronized(timerLock) {
            val generation = ++expiryGeneration
            val old = expiry; expiry = null; old?.cancel()
            if (!active()) return
            val work = managed.engine.scheduler.on(recipient.entity(), ticks) {
                if (active() && synchronized(timerLock) { expiryGeneration == generation }) remove(SkillRemovalReason.EXPIRED)
                Unit
            }
            expiry = work
            work.future.whenComplete { _, error -> if (error != null && active() && synchronized(timerLock) { expiry === work && generation == expiryGeneration }) remove(SkillRemovalReason.ACTOR_RETIRED) }
        }
    }
    internal fun reapply(next: AuraOptions, context: PlatformExecutionContext): CompletableFuture<SkillManagedHandle> {
        val work = managed.engine.scheduler.on(recipient.entity()) {
            context.state.checkActive(); context.frame.checkActive()
            if (!active()) throw CancellationException("Aura is no longer active")
            if (clock() - born >= MAX_NANOS) { remove(SkillRemovalReason.EXPIRED); throw CancellationException("Aura reached its 24-hour lifetime") }
            if (stacks < minOf(options.stackLimit, next.stackLimit)) stackCount.incrementAndGet()
            if (next.refresh) {
                val age = (clock() - born).coerceAtLeast(0)
                refreshAge = minOf(MAX_NANOS, age + next.durationTicks * 50_000_000L)
                chargeCount.set(next.charges)
                val remaining = ((MAX_NANOS - age) / 50_000_000L).coerceAtLeast(1)
                expiration(minOf(next.durationTicks, remaining))
            }
            changeCounter.incrementAndGet()
            this as SkillManagedHandle
        }
        return context.track(work).thenCompose { handle -> changed().thenApply { outcome ->
            if (!outcome.isSuccess) { if (active()) remove(failureReason(outcome)); throw SkillProviderException(outcome) }; handle
        } }
    }
    fun addCharges(delta: Int, context: PlatformExecutionContext?): CompletableFuture<SkillResult> {
        val work = managed.engine.scheduler.on(recipient.entity()) {
            context?.state?.checkActive(); context?.frame?.checkActive()
            if (!active()) SkillResult.of(SkillResult.Status.CANCELLED, "Aura is inactive") else {
                val count = chargeCount.get().toLong() + delta
                require(count <= 1_000_000) { "Aura charge limit is 1000000" }
                chargeCount.set(count.coerceAtLeast(0).toInt()); changeCounter.incrementAndGet()
                if (count <= 0) removeFrom(SkillRemovalReason.CHARGES_EXHAUSTED, context?.takeIf { it.state.managedState === managed }?.frame)
                SkillResult.success()
            }
        }
        return (context?.track(work) ?: work.future).thenCompose { outcome ->
            if (!outcome.isSuccess || !active()) CompletableFuture.completedFuture(outcome)
            else changed().thenApply { updated -> if (!updated.isSuccess && active()) remove(failureReason(updated)); updated }
        }
    }
    internal fun consumeLocal(amount: Int, frame: InvocationFrame) {
        check(managed.engine.scheduler.owns(recipient.entity()))
        require(amount in 0..1_000_000)
        if (amount > 0) {
            chargeCount.set((charges - amount).coerceAtLeast(0)); changeCounter.incrementAndGet()
            if (charges == 0) removeFrom(SkillRemovalReason.CHARGES_EXHAUSTED, frame) else deferChanged()
        }
    }
    override fun remove(reason: SkillRemovalReason) = removeFrom(reason)
    fun removeFrom(reason: SkillRemovalReason, currentFrame: InvocationFrame? = null) {
        val accepted = synchronized(phase) {
            if (phase.get() != 0) false else { removal.set(managed.lease.reason ?: reason); phase.set(1); true }
        }
        if (!accepted) return
        cancelTimers(); managed.cancelCallbacks(currentFrame); managed.releaseOwned(); removed(this)
        val callable = removal.get() in setOf(SkillRemovalReason.EXPIRED, SkillRemovalReason.DISPELLED, SkillRemovalReason.OVERWRITTEN, SkillRemovalReason.CHARGES_EXHAUSTED) && managed.lease.active && definition.revision.available()
        if (callable) managed.execute(definition.onEnd, values()).whenComplete { value, error -> dispose(if (error == null) value else PlatformExecutionState.failure(error)) }
        else dispose(SkillResult.success())
    }
    override fun subscribeTicks(interval: Long, hooks: List<CompiledSkillMechanic>): AutoCloseable {
        require(interval in 1..1_728_000); require(hooks.isNotEmpty())
        val lease = managed.platform.managed.reserve(definition.revision, recipient, managed.state.root.dependencies, managed.lease.relatedActors)
        val pending = AtomicReference<ScheduledWork<Unit>?>()
        lease.onClose { pending.getAndSet(null)?.cancel() }
        managed.own(lease)
        fun schedule() {
            if (!active() || !lease.active) return
            val work = managed.engine.scheduler.on(recipient.entity(), interval) {
                pending.set(null)
                if (active() && lease.active) {
                    if (!InvocationSnapshots.valid(recipient.entity())) remove(SkillRemovalReason.ACTOR_RETIRED)
                    else managed.execute(hooks, values()).whenComplete { outcome, error ->
                        if (active() && lease.active) {
                            val result = if (error == null) outcome else PlatformExecutionState.failure(error)
                            if (result.isSuccess) schedule() else remove(failureReason(result))
                        }
                    }
                }
                Unit
            }
            pending.set(work)
            if (!active() || !lease.active) pending.getAndSet(null)?.cancel()
            work.future.whenComplete { _, error -> if (error != null && lease.active && active()) remove(SkillRemovalReason.ACTOR_RETIRED) }
        }
        schedule(); return lease
    }
    private fun dispose(result: SkillResult) {
        phase.set(2); managed.remove(removal.get() ?: SkillRemovalReason.CANCELLED); started.completeExceptionally(CancellationException("Aura ended before start completed")); finished.complete(result)
    }
    private fun cancelTimers() = synchronized(timerLock) {
        expiryGeneration++
        val expiration = expiry; val interval = ticking; val reconcile = reconciliation
        expiry = null; ticking = null; reconciliation = null
        expiration?.cancel(); interval?.cancel(); reconcile?.cancel()
    }
    internal fun failureReason(result: SkillResult) = when (result.status()) {
        SkillResult.Status.BUDGET_EXCEEDED -> SkillRemovalReason.BUDGET_EXCEEDED
        SkillResult.Status.CANCELLED -> SkillRemovalReason.CANCELLED
        SkillResult.Status.DEPENDENCY_UNAVAILABLE -> if (definition.revision.isActive) SkillRemovalReason.OWNER_UNLOADED else SkillRemovalReason.DEFINITION_RELOADED
        else -> SkillRemovalReason.ERROR
    }
    companion object { private const val MAX_NANOS = 86_400_000_000_000L }
}
