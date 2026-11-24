package com.keopsfenks.arcana.skills.execution

import com.keopsfenks.arcana.core.skills.SkillRegistration
import com.keopsfenks.arcana.core.skills.SkillResult
import com.keopsfenks.arcana.skills.compile.SkillSnapshot
import java.util.UUID
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class EffectStepLimitException : IllegalStateException("Skill execution exceeds 1024 effect steps")

class ExecutionState(val playerId: UUID, val snapshot: SkillSnapshot) {
    val result = CompletableFuture<SkillResult>()
    private val cancelled = AtomicBoolean(false)
    private val steps = AtomicInteger(0)
    val appliedSteps: Int get() = steps.get()
    @Volatile var platformState: com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionState? = null
    private val work = ConcurrentHashMap.newKeySet<ScheduledWork<*>>()
    val pendingWork: Int get() = work.size
    val dependencies = ConcurrentHashMap.newKeySet<SkillRegistration>()

    fun checkActive() { if (cancelled.get() || result.isDone) throw CancellationException("Skill execution finished or cancelled") }
    fun step() {
        checkActive()
        if (steps.incrementAndGet() > 1024) throw EffectStepLimitException()
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
