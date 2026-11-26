package com.keopsfenks.arcana.skills.platform.execution

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.execution.*
import com.keopsfenks.arcana.skills.platform.sources.DefinitionSnapshot
import com.keopsfenks.arcana.skills.platform.values.InvocationFrame
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

class PlatformBudgetException(message: String) : IllegalStateException(message)
class PlatformDependencyException(message: String) : IllegalStateException(message)

/** Native and legacy descendants borrow the exact same root state and scheduled work set. */
class PlatformExecutionState @JvmOverloads constructor(val root: ExecutionState, val snapshot: DefinitionSnapshot, private val available: (SkillRegistration) -> Boolean = { it.isActive }) {
    @Volatile var traceId: java.util.UUID? = null
    @Volatile var managedState: com.keopsfenks.arcana.skills.platform.managed.ManagedExecutionState? = null
    private val frames = ConcurrentHashMap.newKeySet<InvocationFrame>()
    private var branches = AtomicInteger()
    internal fun inheritBranchBudget(source: PlatformExecutionState) {
        check(branches.get() == 0) { "Branch budget must be inherited before use" }
        branches = source.branches
    }
    init { root.result.whenComplete { result, error ->
        val cancelled = error != null || result?.status() == com.keopsfenks.arcana.core.skills.SkillResult.Status.CANCELLED
        frames.toList().forEach { if (cancelled) it.close() else it.complete() }; frames.clear()
    } }
    fun checkActive() {
        root.checkActive()
        if (root.appliedSteps > root.stepLimit) throw PlatformBudgetException("Skill execution exceeds ${root.stepLimit} effect steps")
        managedState?.checkBudget()
        if (root.dependencies.any { !available(it) }) throw PlatformDependencyException("A definition or provider was removed")
    }
    fun step() {
        checkActive()
        try { root.step() } catch (error: IllegalStateException) {
            if (root.appliedSteps > root.stepLimit) throw PlatformBudgetException("Skill execution exceeds ${root.stepLimit} effect steps")
            throw error
        }
    }
    fun frame(frame: InvocationFrame): InvocationFrame {
        checkActive(); frames.add(frame); frame.onClose { frames.remove(frame) }
        try { checkActive() } catch (error: Throwable) { release(frame, cancelled = true); throw error }
        return frame
    }
    fun release(frame: InvocationFrame, cancelled: Boolean = false) { frames.remove(frame); if (cancelled) frame.close() else frame.complete() }
    fun reserveBranches(count: Int): AutoCloseable {
        require(count > 0) { "Parallel fan-out must be positive" }
        if (count > 16) throw PlatformBudgetException("Parallel fan-out exceeds 16")
        checkActive()
        val tracker = branches
        if (tracker.addAndGet(count) > 16) { tracker.addAndGet(-count); throw PlatformBudgetException("Parallel fan-out exceeds 16") }
        val closed = java.util.concurrent.atomic.AtomicBoolean()
        return AutoCloseable { if (closed.compareAndSet(false, true)) tracker.addAndGet(-count) }
    }
    fun <T> track(work: ScheduledWork<T>) = root.track(work)
    companion object {
        fun failure(error: Throwable): SkillResult {
            var cause = error
            while ((cause is CompletionException || cause is ExecutionException) && cause.cause != null) cause = cause.cause!!
            return SkillResult.of(when (cause) {
                is CancellationException -> SkillResult.Status.CANCELLED
                is PlatformBudgetException -> SkillResult.Status.BUDGET_EXCEEDED
                is EffectStepLimitException -> SkillResult.Status.BUDGET_EXCEEDED
                is PlatformDependencyException -> SkillResult.Status.DEPENDENCY_UNAVAILABLE
                is MissingSkillTargetException -> SkillResult.Status.NO_TARGET
                is com.keopsfenks.arcana.skills.platform.legacy.LegacyActorException -> return cause.result
                is com.keopsfenks.arcana.core.skills.platform.SkillProviderException -> return cause.result()
                else -> SkillResult.Status.ERROR
            }, cause.message ?: cause.javaClass.simpleName)
        }
    }
}
