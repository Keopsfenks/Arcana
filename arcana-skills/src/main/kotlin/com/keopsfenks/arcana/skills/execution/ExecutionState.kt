package com.keopsfenks.arcana.skills.execution

import com.keopsfenks.arcana.core.skills.SkillRegistration
import com.keopsfenks.arcana.core.skills.SkillResult
import com.keopsfenks.arcana.skills.compile.SkillSnapshot
import java.util.UUID
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class EffectStepLimitException @JvmOverloads constructor(limit: Int = 1024) : IllegalStateException("Skill execution exceeds $limit effect steps")

class ExecutionState @JvmOverloads constructor(val playerId: UUID, val snapshot: SkillSnapshot, val stepLimit: Int = 1024, val stepBudget: ExecutionStepBudget = ExecutionStepBudget(stepLimit)) {
    init { require(stepLimit in 1..65536 && stepBudget.limit == stepLimit) }
    val id: UUID = UUID.randomUUID()
    val result = CompletableFuture<SkillResult>()
    private val cancelled = AtomicBoolean(false)
    private val explicitCancellation = AtomicBoolean(false)
    private val cancellationCallbacks = java.util.concurrent.ConcurrentLinkedQueue<() -> Unit>()
    init { result.whenComplete { outcome, error -> if (error != null || outcome?.status() == SkillResult.Status.CANCELLED) signalCancellation() else cancellationCallbacks.clear() } }
    fun onCancel(action: () -> Unit): AutoCloseable {
        cancellationCallbacks.add(action)
        if ((explicitCancellation.get() || result.isDone && (result.isCompletedExceptionally || result.getNow(null)?.status() == SkillResult.Status.CANCELLED)) && cancellationCallbacks.remove(action)) action()
        else if (result.isDone) cancellationCallbacks.remove(action)
        return AutoCloseable { cancellationCallbacks.remove(action) }
    }
    private fun signalCancellation() {
        if (explicitCancellation.compareAndSet(false, true)) while (true) (cancellationCallbacks.poll() ?: break).invoke()
    }
    val appliedSteps: Int get() = stepBudget.applied
    @Volatile var platformState: com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionState? = null
    private val work = ConcurrentHashMap.newKeySet<ScheduledWork<*>>()
    val pendingWork: Int get() = work.size
    val dependencies = ConcurrentHashMap.newKeySet<SkillRegistration>()

    fun checkActive() { if (cancelled.get() || result.isDone) throw CancellationException("Skill execution finished or cancelled") }
    fun step() {
        checkActive()
        stepBudget.step()
        check(dependencies.all { it.isActive }) { "Skill component dependency is unavailable" }
    }

    fun <T> track(task: ScheduledWork<T>): CompletableFuture<T> {
        work.add(task)
        task.future.whenComplete { _, _ -> work.remove(task) }
        if (cancelled.get() || result.isDone) task.cancel()
        return task.future
    }

    fun cancel(reason: String) {
        if (cancelled.compareAndSet(false, true)) {
            signalCancellation()
            result.complete(SkillResult.of(SkillResult.Status.CANCELLED, reason))
            release()
        }
    }

    fun release() {
        cancelled.set(true)
        work.toList().forEach { it.cancel() }
        work.clear()
    }
}
