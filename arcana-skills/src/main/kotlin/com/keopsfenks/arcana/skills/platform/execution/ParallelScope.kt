package com.keopsfenks.arcana.skills.platform.execution

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.values.InvocationFrame
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReferenceArray

data class PlatformBranch(val scope: InvocationFrame, val ready: CompletableFuture<PlatformExecutionContext>)

/** Ordered outcomes with scoped cancellation, including work still capturing owner snapshots. */
class ParallelScope(private val context: PlatformExecutionContext) {
    fun execute(branches: List<List<CompiledSkillMechanic>>, failFast: Boolean = true): CompletableFuture<List<SkillResult>> {
        val immutable = java.util.List.copyOf(branches.map { java.util.List.copyOf(it) })
        if (immutable.isEmpty()) return CompletableFuture.completedFuture(emptyList())
        val permit = context.state.reserveBranches(immutable.size)
        val slots = List(immutable.size) { CompletableFuture<SkillResult>() }
        val scopes = AtomicReferenceArray<PlatformBranch?>(immutable.size)
        val stopped = AtomicBoolean()
        val result = CompletableFuture<List<SkillResult>>()
        result.whenComplete { _, _ -> permit.close() }
        fun close(branch: PlatformBranch, cancelled: Boolean = false) { context.state.release(branch.scope, cancelled); branch.ready.cancel(false) }
        fun complete(index: Int, outcome: SkillResult) {
            slots[index].complete(outcome)
            if (!outcome.isSuccess && (failFast || hard(outcome)) && stopped.compareAndSet(false, true)) {
                for (other in slots.indices) if (other != index) {
                    scopes.get(other)?.let { close(it, cancelled = true) }
                    slots[other].complete(SkillResult.of(SkillResult.Status.CANCELLED, "Sibling branch stopped"))
                }
            }
            if (slots.all { it.isDone }) result.complete(java.util.List.copyOf(slots.map { it.join() }))
        }
        for (index in immutable.indices) {
            if (stopped.get()) break
            try {
                val branch = context.branch(context.invocation())
                scopes.set(index, branch)
                if (stopped.get()) { close(branch, cancelled = true); continue }
                branch.ready.thenCompose { child ->
                    if (stopped.get()) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.CANCELLED, "Sibling branch stopped"))
                    else child.execute(immutable[index]).toCompletableFuture()
                }.whenComplete { outcome, error ->
                    close(branch)
                    complete(index, if (error == null) outcome else PlatformExecutionState.failure(error))
                }
            } catch (error: Throwable) { complete(index, PlatformExecutionState.failure(error)) }
        }
        return result
    }
    fun forEach(inputs: List<SkillInvocation>, effects: List<CompiledSkillMechanic>, failFast: Boolean = false): CompletableFuture<List<SkillResult>> {
        if (inputs.size > 256) throw PlatformBudgetException("Foreach exceeds 256 targets")
        val body = java.util.List.copyOf(effects)
        var result = CompletableFuture.completedFuture(emptyList<SkillResult>())
        for (input in java.util.List.copyOf(inputs)) result = result.thenCompose { previous ->
            if (previous.any { hard(it) || failFast && !it.isSuccess }) CompletableFuture.completedFuture(previous)
            else {
                val branch = context.branch(input)
                branch.ready.thenCompose {
                    if (input.target().isPresent && it.invocation().target().isEmpty) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.NO_TARGET, "Selected target retired"))
                    else it.execute(body).toCompletableFuture()
                }.handle { outcome, error ->
                    context.state.release(branch.scope); branch.ready.cancel(false)
                    previous + if (error == null) outcome else PlatformExecutionState.failure(error)
                }
            }
        }
        return result.thenApply { java.util.List.copyOf(it) }
    }
    companion object {
        fun hard(result: SkillResult) = result.status() in setOf(SkillResult.Status.CANCELLED, SkillResult.Status.BUDGET_EXCEEDED, SkillResult.Status.DEPENDENCY_UNAVAILABLE, SkillResult.Status.PROVIDER_UNAVAILABLE)
        fun aggregate(results: List<SkillResult>): SkillResult = (results.firstOrNull { hard(it) && it.status() != SkillResult.Status.CANCELLED }
            ?: results.firstOrNull { !it.isSuccess && it.status() != SkillResult.Status.CANCELLED } ?: results.firstOrNull { !it.isSuccess })?.let { first ->
            SkillResult.of(first.status(), "${results.count { !it.isSuccess }}/${results.size} branches failed: ${first.message()}")
        } ?: SkillResult.success()
    }
}
