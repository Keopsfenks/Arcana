package com.keopsfenks.arcana.skills.platform.projectiles

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.execution.ScheduledWork
import com.keopsfenks.arcana.skills.platform.execution.*
import com.keopsfenks.arcana.skills.platform.managed.ManagedExecutionState
import org.bukkit.Location
import org.bukkit.World
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*

class ProjectileInstance internal constructor(
    val managed: ManagedExecutionState, private val input: SkillInvocation,
    initial: SkillLocation, val options: ProjectileOptions, private val hooks: ProjectileHooks,
    private val query: SkillTargetQuery, private val worlds: (UUID) -> World?, private val removed: (ProjectileInstance) -> Unit
) : SkillManagedHandle {
    private val phase = AtomicInteger(); private val terminal = AtomicReference<SkillRemovalReason?>()
    private val timers = ConcurrentHashMap.newKeySet<ScheduledWork<*>>()
    private val ledger = ProjectileHitLedger(options.maxHits, options.immuneTicks)
    private var age = 0L; private var bounceLeft = options.bounces; private var pierceLeft = options.pierce
    @Volatile var position: SkillLocation = initial; private set
    @Volatile var velocity: SkillVector = ProjectileMotion.initial(options); private set
    val started = CompletableFuture<SkillManagedHandle>(); val finished = CompletableFuture<SkillResult>()
    override fun id() = managed.lease.id
    override fun skill() = managed.revision.key
    override fun revision() = managed.revision.revisionId
    override fun actor() = input.actor()
    override fun active() = phase.get() == 0 && managed.lease.active
    override fun reason(): Optional<SkillRemovalReason> = Optional.ofNullable(terminal.get())
    init {
        managed.deadlineAction = { remove(SkillRemovalReason.EXPIRED) }
        managed.lease.onClose {
            val disposed = synchronized(phase) {
                if (phase.get() == 2) false else { terminal.compareAndSet(null, managed.lease.reason ?: SkillRemovalReason.CANCELLED); phase.set(2); true }
            }
            if (disposed) {
                cancelTimers(); managed.releaseOwned(); removed(this)
                started.completeExceptionally(CancellationException("Projectile retired during initialization"))
                finished.complete(SkillResult.of(SkillResult.Status.CANCELLED, "Projectile retired"))
            }
        }
    }
    private fun view(at: SkillLocation = position, hit: SkillActor? = input.target().orElse(null)) = input.derive()
        .origin(input.origin().orElse(position)).location(at).target(hit).selection(SkillSelection.empty()).build()
    private fun values() = mapOf<String, Any>("projectile_age" to age, "projectile_hits" to ledger.total, "projectile_bounces" to options.bounces-bounceLeft)
    internal fun start() {
        if (!active()) return
        // Simulation includes its last movement tick; one bounded tick also retires stalled I/O.
        timer(options.lifetimeTicks + 1) { remove(SkillRemovalReason.EXPIRED) }
        managed.execute(hooks.onStart, values(), view()).whenComplete { result, error ->
            if (!active()) return@whenComplete
            if (accept(result, error)) { started.complete(this); schedule() }
        }
    }
    private fun timer(delay: Long, action: () -> Unit) {
        if (!active()) return
        val work = managed.engine.scheduler.on(actor().entity(), delay) {
            if (active()) {
                if (!InvocationSnapshots.valid(actor().entity())) remove(SkillRemovalReason.ACTOR_RETIRED) else action()
            }; Unit
        }
        timers.add(work)
        work.future.whenComplete { _, error -> timers.remove(work); if (error != null && active()) remove(SkillRemovalReason.ACTOR_RETIRED) }
        if (!active()) work.cancel()
    }
    private fun schedule() = timer(1) { step() }
    private fun step() {
        if (!active()) return
        try { managed.callback() } catch (error: Throwable) { fail(error); return }
        val target = if (options.homing > 0) input.target().orElse(null) else null
        val captured = if (target == null) CompletableFuture.completedFuture(Optional.empty<SkillEntitySnapshot>()) else query.snapshot(target).toCompletableFuture()
        captured.thenCompose { snapshot ->
            if (!active()) return@thenCompose CompletableFuture.completedFuture(Unit)
            if (target != null && (snapshot.isEmpty || snapshot.get().location().world() != position.world())) {
                remove(SkillRemovalReason.ACTOR_RETIRED); return@thenCompose CompletableFuture.completedFuture(Unit)
            }
            val toward = snapshot.map { SkillVector(it.location().x()-position.x(), it.location().y()-position.y(), it.location().z()-position.z()) }.orElse(null)
            synchronized(phase) { if (active()) { age++; velocity = ProjectileMotion.advance(velocity, options, toward) } }
            segment(position, ProjectileMotion.move(position, velocity), 0)
        }.whenComplete { _, error ->
            if (error != null) fail(error)
            else if (active()) managed.execute(hooks.onTick, values(), view()).whenComplete { result, failure ->
                if (active() && accept(result, failure)) { if (age >= options.lifetimeTicks) remove(SkillRemovalReason.EXPIRED) else schedule() }
            }
        }
    }
    private fun segment(from: SkillLocation, to: SkillLocation, depth: Int): CompletableFuture<Unit> {
        if (!active()) return CompletableFuture.completedFuture(Unit)
        if (depth > 100) return CompletableFuture.failedFuture(PlatformBudgetException("Projectile exceeded 100 segment bounces"))
        return query.loaded(CollisionResolver.bounds(from, to, options.radius)).thenCompose { loaded ->
            if (!loaded) { remove(SkillRemovalReason.EXPIRED); CompletableFuture.completedFuture(emptyList<SkillCollision>()) }
            else if (!active()) CompletableFuture.completedFuture(emptyList())
            else query.collisions(from, to, options.radius)
        }.thenCompose { contacts ->
            if (!active()) CompletableFuture.completedFuture(Unit)
            else process(CollisionResolver.ordered(contacts, actor(), options.hitCaster), 0, from, to, depth)
        }.toCompletableFuture()
    }
    private fun process(contacts: List<SkillCollision>, index: Int, from: SkillLocation, to: SkillLocation, depth: Int): CompletableFuture<Unit> {
        if (!active()) return CompletableFuture.completedFuture(Unit)
        if (index >= contacts.size) return commit(to)
        val contact = contacts[index]
        require(contact.location().world() == from.world()) { "Projectile contact crosses worlds" }
        val target = contact.actor().orElse(null)
        var accepted = false
        val checked = CompiledSkillMechanic { context ->
            if (!active() || target != null && context.invocation().target().orElse(null) != target || !hooks.hitCondition.test(context.invocation()) || !ledger.accept(target?.id(), age)) CompletableFuture.completedFuture(SkillResult.success())
            else { accepted = true; context.execute(hooks.onHit) }
        }
        return commit(contact.location()).thenCompose {
            if (!active()) CompletableFuture.completedFuture(SkillResult.success())
            else managed.execute(listOf(checked), values(), view(contact.location(), target))
        }.thenCompose { outcome ->
            if (!active()) CompletableFuture.completedFuture(Unit)
            else if (!outcome.isSuccess) { remove(failureReason(outcome)); CompletableFuture.completedFuture(Unit) }
            else if (accepted && ledger.exhausted()) { remove(SkillRemovalReason.EXPIRED); CompletableFuture.completedFuture(Unit) }
            else if (target == null) {
                if (bounceLeft <= 0 || ProjectileMotion.length(contact.normal()) < 1e-9) { remove(SkillRemovalReason.EXPIRED); CompletableFuture.completedFuture(Unit) }
                else {
                    synchronized(phase) { if (active()) { bounceLeft--; velocity = ProjectileMotion.reflect(velocity, contact.normal(), options.bounceEnergy) } }
                    val start = ProjectileMotion.move(contact.location(), ProjectileMotion.scale(ProjectileMotion.unit(velocity), 1e-5))
                    val remaining = ProjectileMotion.scale(velocity, 1-contact.fraction())
                    segment(start, ProjectileMotion.move(start, remaining), depth+1)
                }
            } else if (accepted && pierceLeft-- <= 0) { remove(SkillRemovalReason.EXPIRED); CompletableFuture.completedFuture(Unit) }
            else process(contacts, index+1, from, to, depth)
        }.toCompletableFuture()
    }
    /** Commit on the destination region only after the complete swept query has returned. */
    private fun commit(at: SkillLocation): CompletableFuture<Unit> {
        if (!active()) return CompletableFuture.completedFuture(Unit)
        return managed.state.track(managed.engine.scheduler.global { worlds(at.world()) }).thenCompose { world ->
            if (world == null) { remove(SkillRemovalReason.EXPIRED); CompletableFuture.completedFuture(Unit) }
            else managed.state.track(managed.engine.scheduler.at(Location(world, at.x(), at.y(), at.z())) {
                if (active()) {
                    if (!world.isChunkLoaded(kotlin.math.floor(at.x()).toInt() shr 4, kotlin.math.floor(at.z()).toInt() shr 4)) remove(SkillRemovalReason.EXPIRED)
                    else synchronized(phase) { if (active()) position = at }
                }; Unit
            })
        }
    }
    private fun accept(result: SkillResult?, error: Throwable?): Boolean {
        val outcome = if (error != null) PlatformExecutionState.failure(error) else result ?: SkillResult.of(SkillResult.Status.ERROR, "Missing projectile outcome")
        if (!outcome.isSuccess) { remove(failureReason(outcome)); return false }; return true
    }
    private fun fail(error: Throwable) { if (active()) remove(failureReason(PlatformExecutionState.failure(error))) }
    private fun failureReason(result: SkillResult) = when (result.status()) {
        SkillResult.Status.BUDGET_EXCEEDED -> SkillRemovalReason.BUDGET_EXCEEDED
        SkillResult.Status.CANCELLED -> SkillRemovalReason.CANCELLED
        SkillResult.Status.DEPENDENCY_UNAVAILABLE -> SkillRemovalReason.DEFINITION_RELOADED
        else -> SkillRemovalReason.ERROR
    }
    override fun remove(reason: SkillRemovalReason) {
        val accepted = synchronized(phase) { if (phase.get() != 0) false else { terminal.set(managed.lease.reason ?: reason); phase.set(1); true } }
        if (!accepted) return
        cancelTimers(); managed.cancelCallbacks(); managed.releaseOwned(); removed(this)
        if (terminal.get() in setOf(SkillRemovalReason.EXPIRED, SkillRemovalReason.DISPELLED, SkillRemovalReason.OVERWRITTEN, SkillRemovalReason.CHARGES_EXHAUSTED) && managed.lease.active && managed.revision.available()) {
            managed.execute(hooks.onEnd, values()+mapOf("projectile_reason" to terminal.get()!!.name), view()).whenComplete { result, error -> dispose(if (error == null) result else PlatformExecutionState.failure(error)) }
        } else dispose(SkillResult.success())
    }
    private fun dispose(result: SkillResult) {
        phase.set(2); managed.remove(terminal.get() ?: SkillRemovalReason.CANCELLED)
        started.completeExceptionally(CancellationException("Projectile ended before start completed")); finished.complete(result)
    }
    private fun cancelTimers() { timers.toList().forEach { it.cancel() }; timers.clear() }
}
