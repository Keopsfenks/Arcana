package com.keopsfenks.arcana.skills.platform.managed

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.execution.ScheduledWork
import com.keopsfenks.arcana.skills.platform.execution.*
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*

/** Finite sequential callbacks for beams and native timed leases, without aura matching/stacking. */
class ManagedPulse private constructor(
    val managed: ManagedExecutionState, private val duration: Long, private val period: Long,
    private val onStart: List<CompiledSkillMechanic>, private val pulse: List<CompiledSkillMechanic>, private val onEnd: List<CompiledSkillMechanic>
) : SkillManagedHandle {
    private val phase = AtomicInteger(); private val terminal = AtomicReference<SkillRemovalReason?>()
    private val timers = ConcurrentHashMap.newKeySet<ScheduledWork<*>>()
    val started = CompletableFuture<SkillManagedHandle>(); val finished = CompletableFuture<SkillResult>()
    override fun id() = managed.lease.id
    override fun skill() = managed.revision.key
    override fun revision() = managed.revision.revisionId
    override fun actor() = managed.invocation().actor()
    override fun active() = phase.get() == 0 && managed.lease.active
    override fun reason(): Optional<SkillRemovalReason> = Optional.ofNullable(terminal.get())
    init {
        managed.deadlineAction = { remove(SkillRemovalReason.EXPIRED) }
        managed.lease.onClose {
            if (phase.getAndSet(2) != 2) {
                terminal.compareAndSet(null, managed.lease.reason ?: SkillRemovalReason.CANCELLED)
                cancelTimers(); managed.releaseOwned(); started.completeExceptionally(CancellationException("Managed pulse retired"))
                finished.complete(SkillResult.of(SkillResult.Status.CANCELLED, "Managed pulse retired"))
            }
        }
    }
    private fun timer(delay: Long, action: () -> Unit) {
        if (!active()) return
        val work = managed.engine.scheduler.on(actor().entity(), delay) {
            if (active()) {
                if (!InvocationSnapshots.valid(actor().entity())) remove(SkillRemovalReason.ACTOR_RETIRED) else action()
            }; Unit
        }
        timers.add(work); work.future.whenComplete { _, error -> timers.remove(work); if (error != null && active()) remove(SkillRemovalReason.ACTOR_RETIRED) }
        if (!active()) work.cancel()
    }
    private fun run(first: Boolean) {
        if (!active()) return
        managed.execute(if (first) onStart + pulse else pulse).whenComplete { result, error ->
            if (!active()) return@whenComplete
            val outcome = if (error == null) result else PlatformExecutionState.failure(error)
            if (!outcome.isSuccess) { started.completeExceptionally(SkillProviderException(outcome)); remove(failureReason(outcome)) }
            else { if (first) started.complete(this); if (pulse.isNotEmpty()) timer(period) { run(false) } }
        }
    }
    override fun remove(reason: SkillRemovalReason) {
        if (!phase.compareAndSet(0, 1)) return
        terminal.compareAndSet(null, managed.lease.reason ?: reason)
        cancelTimers(); managed.cancelCallbacks(); managed.releaseOwned()
        if (terminal.get() in setOf(SkillRemovalReason.EXPIRED, SkillRemovalReason.DISPELLED, SkillRemovalReason.OVERWRITTEN, SkillRemovalReason.CHARGES_EXHAUSTED) && managed.lease.active && managed.revision.available()) {
            managed.execute(onEnd).whenComplete { result, error -> dispose(if (error == null) result else PlatformExecutionState.failure(error)) }
        } else dispose(SkillResult.success())
    }
    private fun dispose(result: SkillResult) { phase.set(2); managed.remove(terminal.get() ?: SkillRemovalReason.CANCELLED); started.completeExceptionally(CancellationException("Managed pulse ended before initialization")); finished.complete(result) }
    private fun cancelTimers() { timers.toList().forEach { it.cancel() }; timers.clear() }
    private fun failureReason(result: SkillResult) = when (result.status()) {
        SkillResult.Status.BUDGET_EXCEEDED -> SkillRemovalReason.BUDGET_EXCEEDED
        SkillResult.Status.CANCELLED -> SkillRemovalReason.CANCELLED
        SkillResult.Status.DEPENDENCY_UNAVAILABLE -> SkillRemovalReason.DEFINITION_RELOADED
        else -> SkillRemovalReason.ERROR
    }
    companion object {
        fun start(context: PlatformExecutionContext, duration: Long, period: Long,
            onStart: List<CompiledSkillMechanic> = emptyList(), pulse: List<CompiledSkillMechanic> = emptyList(), onEnd: List<CompiledSkillMechanic> = emptyList()): CompletableFuture<SkillManagedHandle> {
            require(duration in 1..1_728_000 && period in 1..1_728_000)
            val input = context.invocation()
            val managed = ManagedExecutionState.adopt(context, input.target().orElse(input.actor()))
            return try {
                val instance = ManagedPulse(managed, duration, period, java.util.List.copyOf(onStart), java.util.List.copyOf(pulse), java.util.List.copyOf(onEnd))
                instance.timer(duration) { instance.remove(SkillRemovalReason.EXPIRED) }; instance.run(true); instance.started
            } catch (error: Throwable) { managed.close(); CompletableFuture.failedFuture(error) }
        }
    }
}
