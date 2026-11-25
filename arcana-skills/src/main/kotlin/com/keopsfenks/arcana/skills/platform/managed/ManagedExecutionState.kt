package com.keopsfenks.arcana.skills.platform.managed

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.DefaultSkillEngine
import com.keopsfenks.arcana.skills.execution.ExecutionState
import com.keopsfenks.arcana.skills.platform.DefaultSkillPlatform
import com.keopsfenks.arcana.skills.platform.execution.*
import com.keopsfenks.arcana.skills.platform.providers.StatBindings
import com.keopsfenks.arcana.skills.platform.sources.DefinitionRevision
import com.keopsfenks.arcana.skills.platform.values.InvocationFrame
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

/** Adopted work borrows one lifetime budget across every callback, branch and nested cast. */
class ManagedExecutionState private constructor(
    val engine: DefaultSkillEngine, val platform: DefaultSkillPlatform,
    val revision: DefinitionRevision, source: PlatformExecutionContext,
    val lease: ManagedWorkRegistry.ManagedLease, input: SkillInvocation
) : AutoCloseable {
    private class Lineage(val born: Long, val limits: ManagedLimits) {
        val steps = com.keopsfenks.arcana.skills.execution.ExecutionStepBudget(limits.steps)
        val callbacks = AtomicInteger()
    }
    private val lineage: Lineage = source.state.managedState?.lineage ?: Lineage(platform.monotonicClock(), platform.managed.limits)
    val state = PlatformExecutionState(ExecutionState(input.actor().id(), source.state.snapshot.legacy, lineage.limits.steps, lineage.steps), source.state.snapshot, platform::componentActive).also { it.traceId = source.state.traceId }
    val originId = source.state.root.id
    internal val definition = source.definition
    @Volatile var aura: com.keopsfenks.arcana.skills.platform.auras.AuraInstance? = null
        internal set
    private val depth = source.depth
    private val admission = source.admission
    private val base = state.frame(source.frame.detachedCopy(input))
    private val scopes = ConcurrentHashMap.newKeySet<InvocationFrame>()
    private class OwnedCleanup(cleanup: AutoCloseable) : AutoCloseable {
        private val value = java.util.concurrent.atomic.AtomicReference<AutoCloseable?>(cleanup)
        override fun close() { value.getAndSet(null)?.close() }
    }
    private val owned = ConcurrentHashMap.newKeySet<OwnedCleanup>()
    private val ownedSealed = java.util.concurrent.atomic.AtomicBoolean()
    private val callbacks get() = lineage.callbacks
    @Volatile var deadlineAction: (() -> Unit)? = null
        internal set
    val callbackCount get() = callbacks.get()
    init {
        state.root.platformState = state
        state.managedState = this
        state.root.dependencies.add(revision); state.root.dependencies.addAll(lease.dependencies)
        lease.onClose {
            if (lease.reason in setOf(SkillRemovalReason.EXPIRED, SkillRemovalReason.DISPELLED, SkillRemovalReason.OVERWRITTEN, SkillRemovalReason.CHARGES_EXHAUSTED)) state.root.result.complete(SkillResult.success())
            else state.root.cancel("Managed work retired")
        }
        state.root.onCancel { lease.close() }
        val parentFrame = source.frame
        val frameSubscription = java.util.concurrent.atomic.AtomicReference<AutoCloseable?>()
        frameSubscription.set(parentFrame.onCancel { close() })
        parentFrame.onClose { frameSubscription.getAndSet(null)?.close() }
        lease.onClose { frameSubscription.getAndSet(null)?.close() }
        val parentRoot = source.state.root
        val subscription = java.util.concurrent.atomic.AtomicReference<AutoCloseable?>()
        subscription.set(parentRoot.onCancel { close() })
        parentRoot.result.whenComplete { outcome, error ->
            if (error != null || outcome?.status() == SkillResult.Status.CANCELLED) close()
            subscription.getAndSet(null)?.close()
        }
        lease.onClose { subscription.getAndSet(null)?.close() }
    }
    fun checkBudget() { if (callbacks.get() > lineage.limits.callbacks) throw PlatformBudgetException("Managed lifetime exceeds ${lineage.limits.callbacks} callbacks") }
    fun callback() {
        state.checkActive()
        if (callbacks.incrementAndGet() > lineage.limits.callbacks) throw PlatformBudgetException("Managed lifetime exceeds ${lineage.limits.callbacks} callbacks")
    }
    fun own(cleanup: AutoCloseable) {
        val action = OwnedCleanup(cleanup)
        owned.add(action)
        lease.onClose { owned.remove(action); disposeOwned(action) }
        if ((ownedSealed.get() || !lease.active) && owned.remove(action)) disposeOwned(action)
    }
    fun releaseOwned() {
        ownedSealed.set(true)
        owned.toList().forEach { if (owned.remove(it)) disposeOwned(it) }
    }
    private fun disposeOwned(action: OwnedCleanup) {
        try { action.close() } catch (error: Exception) { engine.reportProviderFailure("Could not remove managed native contribution: ${error.message}") }
    }
    fun invocation() = base.invocation
    internal fun localFrame(input: SkillInvocation) = state.frame(base.view(input))
    internal fun eventAdmission(): SkillAdmission = admission ?: object : SkillAdmission {
        override fun id() = lease.id
        override fun skill() = revision.key
        override fun actor() = base.invocation.actor()
        override fun committed() = true // Successful passive activation has no normal admission costs.
    }
    fun cancelCallbacks(except: InvocationFrame? = null) { scopes.toList().filter { except == null || !except.descendsFrom(it) }.forEach { state.release(it, cancelled = true) } }
    fun execute(hooks: List<CompiledSkillMechanic>, values: Map<String, Any> = emptyMap(), input: SkillInvocation = base.invocation): CompletableFuture<SkillResult> {
        val result = CompletableFuture<SkillResult>()
        try {
            callback()
            val seed = state.frame(base.view(input.derive().data(values).build()))
            scopes.add(seed)
            seed.onClose { scopes.remove(seed); result.complete(SkillResult.of(SkillResult.Status.CANCELLED, "Managed callback retired")) }
            val persistent = definition.bindings.filter { it.persistent }.groupBy { it.scope }.mapValues { (_, values) -> values.map { it.key }.toSet() }
            InvocationSnapshots.capture(seed.invocation, engine.scheduler, state, seed, definition.snapshots, platform).thenCompose { captured ->
                StatBindings.capture(captured, definition.stats, state, seed, platform)
            }.thenCompose { captured ->
                val frame = state.frame(seed.view(captured)); val access = platform.variables.access(frame, persistent)
                access.prepare().thenCompose { PlatformExecutionContext(engine, platform, state, frame, definition, depth, access, admission).execute(hooks).toCompletableFuture() }
            }.whenComplete { value, error ->
                result.complete(if (error == null) value else PlatformExecutionState.failure(error)); state.release(seed)
            }
        } catch (error: Throwable) { result.complete(PlatformExecutionState.failure(error)) }
        return result
    }
    fun remove(reason: SkillRemovalReason) { lease.remove(reason) }
    private fun deadline() {
        val remaining = lineage.limits.lifetimeTicks * 50_000_000L - (platform.monotonicClock() - lineage.born)
        if (remaining <= 0) throw CancellationException("Managed lineage reached its 24-hour lifetime")
        val ticks = ((remaining + 49_999_999L) / 50_000_000L).coerceIn(1, 1_728_000)
        val work = engine.scheduler.on(lease.actor.entity(), ticks) {
            try { deadlineAction?.invoke() } finally { lease.remove(SkillRemovalReason.EXPIRED) }
            Unit
        }
        state.track(work).whenComplete { _, error -> if (error != null && !state.root.result.isDone) lease.remove(SkillRemovalReason.ACTOR_RETIRED) }
    }
    override fun close() { lease.close() }
    companion object {
        fun adopt(context: PlatformExecutionContext, target: SkillActor, dependencies: Set<SkillRegistration> = emptySet(), invocation: SkillInvocation = context.invocation()): ManagedExecutionState {
            context.state.checkActive(); context.frame.checkActive()
            require(invocation.actor() == context.invocation().actor() && invocation.parameters() == context.invocation().parameters()) { "Adoption must preserve the executing actor and typed parameters" }
            val revision = context.state.snapshot.entries[context.definition.key] ?: throw PlatformDependencyException("Managed source revision unavailable")
            val subjects = (context.state.managedState?.lease?.relatedActors ?: emptySet()) + context.invocation().actor().id() + target.id()
            if (subjects.size > 256) throw PlatformBudgetException("Managed lineage exceeds 256 related actors")
            val lease = context.platform.managed.reserve(revision, target, context.state.root.dependencies + dependencies, subjects)
            try {
                val managed = ManagedExecutionState(context.engine, context.platform, revision, context, lease, invocation.derive().target(target).build())
                check(context.engine.enroll(managed.state.root)) { "Skill engine stopped" }
                managed.state.checkActive(); managed.deadline(); return managed
            } catch (error: Throwable) { lease.close(); throw error }
        }
    }
}
