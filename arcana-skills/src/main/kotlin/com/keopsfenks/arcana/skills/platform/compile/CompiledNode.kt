package com.keopsfenks.arcana.skills.platform.compile

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionContext
import com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionState
import com.keopsfenks.arcana.skills.platform.targets.TargetSelectionPolicy
import java.util.concurrent.*
import java.util.function.DoubleSupplier

/** One immutable compiled node. Branches share root work/steps; sequential views share locals only. */
class CompiledNode(private val mechanic: CompiledSkillMechanic, val options: NodeOptions) : CompiledSkillMechanic {
    override fun execute(execution: SkillExecutionContext): CompletionStage<SkillResult> {
        if (execution !is PlatformExecutionContext) {
            return if (!options.configured) mechanic.execute(execution) else completed(SkillResult.Status.UNSUPPORTED_CONTEXT, "Node options require an Arcana execution context")
        }
        if (!options.configured) {
            execution.state.step()
            return mechanic.execute(execution)
        }
        return try {
            val input = execution.invocation()
            val repeats = options.repeat.evaluate(input).toInt()
            var result = execution.delay(options.delay.evaluate(input).toLong()).toCompletableFuture()
            repeat(repeats) { iteration -> result = result.thenCompose { previous ->
                if (!previous.isSuccess) CompletableFuture.completedFuture(previous)
                else (if (iteration == 0) CompletableFuture.completedFuture(SkillResult.success()) else execution.delay(options.interval.evaluate(execution.invocation()).toLong()).toCompletableFuture())
                    .thenCompose { delayed -> if (!delayed.isSuccess) CompletableFuture.completedFuture(delayed) else select(execution) }
            } }
            result.handle { value, error -> if (error == null) value else PlatformExecutionState.failure(error) }.thenApply(::failurePolicy)
        } catch (error: Throwable) { CompletableFuture.completedFuture(failurePolicy(PlatformExecutionState.failure(error))) }
    }
    private fun select(execution: PlatformExecutionContext): CompletableFuture<SkillResult> {
        execution.state.checkActive()
        val input = execution.invocation()
        val targeter = options.targeter ?: return apply(execution, input)
        val query = execution.services().require(SkillTargetQuery::class.java)
        return targeter.select(SkillTargetingContext(input, query, DoubleSupplier { execution.engine.random() })).thenCompose { selected ->
            if (selected.entities().isEmpty() && selected.locations().isEmpty()) { execution.state.step(); return@thenCompose completed(SkillResult.Status.NO_TARGET, "Targeter selected no targets") }
            var result = CompletableFuture.completedFuture(SkillResult.success())
            for (actor in selected.entities()) result = result.thenCompose { previous ->
                if (!previous.isSuccess) CompletableFuture.completedFuture(previous) else query.snapshot(actor).thenCompose { captured ->
                    if (captured.isEmpty) completed(SkillResult.Status.NO_TARGET, "Selected target retired") else apply(execution, TargetSelectionPolicy.selected(input, captured.get()), true)
                }
            }
            for (location in selected.locations()) result = result.thenCompose { previous ->
                if (!previous.isSuccess) CompletableFuture.completedFuture(previous) else apply(execution, TargetSelectionPolicy.selected(input, location), true)
            }
            result
        }.toCompletableFuture()
    }
    private fun apply(parent: PlatformExecutionContext, input: SkillInvocation, isolateLocals: Boolean = false): CompletableFuture<SkillResult> {
        parent.state.step()
        val mutated = options.mutators.fold(input) { current, mutator -> mutator.mutate(current) }
        return parent.derived(mutated, isolateLocals).thenCompose { execution ->
            val result = if (mutated.target().isPresent && execution.invocation().target().isEmpty) completed(SkillResult.Status.NO_TARGET, "Selected target retired") else
                parent.state.track(parent.engine.scheduler.on(execution.invocation().actor().entity()) {
                    parent.state.checkActive(); execution.frame.checkActive()
                    val invocation = execution.invocation()
                    when {
                        !options.conditions.test(invocation) -> SkillResult.of(SkillResult.Status.CONDITION_FAILED, "Node conditions did not match")
                        !chance(parent, options.chance.evaluate(invocation)) -> SkillResult.of(SkillResult.Status.CHANCE_FAILED, "Node chance did not succeed")
                        else -> SkillResult.success()
                    }
                }).thenCompose { admission -> if (admission.isSuccess) mechanic.execute(execution).toCompletableFuture() else CompletableFuture.completedFuture(admission) }
            result.whenComplete { _, _ -> parent.state.release(execution.frame) }
        }
    }
    private fun chance(execution: PlatformExecutionContext, value: Double): Boolean {
        if (value <= 0) return false
        if (value >= 100) return true
        val roll = execution.engine.random().also { require(it.isFinite() && it >= 0 && it < 1) { "Random source must return a finite value in [0,1)" } }
        return roll * 100 < value
    }
    private fun failurePolicy(result: SkillResult): SkillResult = if (options.skipFailure && result.status() !in setOf(SkillResult.Status.CANCELLED, SkillResult.Status.BUDGET_EXCEEDED, SkillResult.Status.DEPENDENCY_UNAVAILABLE, SkillResult.Status.PROVIDER_UNAVAILABLE)) SkillResult.success() else result
    private fun completed(status: SkillResult.Status, message: String) = CompletableFuture.completedFuture(SkillResult.of(status, message))
}
