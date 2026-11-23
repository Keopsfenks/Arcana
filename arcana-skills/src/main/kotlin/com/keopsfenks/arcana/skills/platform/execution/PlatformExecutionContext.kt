package com.keopsfenks.arcana.skills.platform.execution

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.DefaultSkillEngine
import com.keopsfenks.arcana.skills.platform.DefaultSkillPlatform
import com.keopsfenks.arcana.skills.platform.compile.CompiledPlatformSkill
import com.keopsfenks.arcana.skills.platform.values.InvocationFrame
import com.keopsfenks.arcana.skills.platform.values.VariableStore
import com.keopsfenks.arcana.skills.platform.targets.RegionQuery
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import java.util.concurrent.*
import java.util.function.Function

class PlatformExecutionContext(
    val engine: DefaultSkillEngine,
    val platform: DefaultSkillPlatform,
    val state: PlatformExecutionState,
    val frame: InvocationFrame,
    val definition: CompiledPlatformSkill,
    val depth: Int,
    val variables: VariableStore.VariableAccess
) : SkillExecutionContext {
    private val query: SkillTargetQuery = RegionQuery(engine.scheduler, state = state)
    override fun invocation(): SkillInvocation { state.checkActive(); return frame.snapshot(definition.bindings, variables) }
    override fun services(): SkillServices = SkillServices.of(mapOf<Class<*>, Any>(SkillVariables::class.java to variables, SkillTargetQuery::class.java to query))
    override fun on(actor: SkillActor, action: Function<LivingEntity, SkillResult>): CompletionStage<SkillResult> =
        state.track(engine.scheduler.on(actor.entity()) {
            state.checkActive(); frame.checkActive()
            if (!InvocationSnapshots.valid(actor.entity())) SkillResult.of(SkillResult.Status.NO_TARGET, "Actor target is unavailable") else action.apply(actor.entity())
        }).handle { value, error ->
            state.checkActive(); frame.checkActive()
            if (error == null) value else if (PlatformExecutionState.failure(error).status() == SkillResult.Status.CANCELLED && actor != frame.invocation.actor())
                SkillResult.of(SkillResult.Status.NO_TARGET, "Selected actor retired") else throw CompletionException(error)
        }
    override fun at(location: SkillLocation, action: Function<Location, SkillResult>): CompletionStage<SkillResult> =
        state.track(engine.scheduler.global { state.checkActive(); Bukkit.getWorld(location.world()) }).thenCompose { world ->
            if (world == null) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.NO_TARGET, "World is unavailable"))
            else {
                val target = Location(world, location.x(), location.y(), location.z(), location.yaw(), location.pitch())
                state.track(engine.scheduler.at(target) { state.checkActive(); frame.checkActive(); action.apply(target) })
            }
        }
    override fun execute(mechanics: List<CompiledSkillMechanic>): CompletionStage<SkillResult> {
        val immutable = java.util.List.copyOf(mechanics)
        // Schedule a bounded drain instead of composing a synchronous 1024-deep future chain.
        val result = CompletableFuture<SkillResult>()
        fun drain(index: Int) {
            try {
                state.checkActive(); frame.checkActive()
                var cursor = index
                while (cursor < immutable.size && !result.isDone) {
                    state.step()
                    val next = immutable[cursor++].execute(this).toCompletableFuture()
                    if (next.isDone) {
                        val outcome = next.join()
                        if (!outcome.isSuccess) { result.complete(outcome); return }
                    } else {
                        val resume = cursor
                        next.whenComplete { outcome, error ->
                            if (error != null) result.completeExceptionally(error)
                            else if (!outcome.isSuccess) result.complete(outcome)
                            else drain(resume)
                        }
                        return
                    }
                }
                if (!result.isDone) result.complete(SkillResult.success())
            } catch (error: Throwable) { result.completeExceptionally(error) }
        }
        drain(0)
        return result
    }
    override fun delay(ticks: Long): CompletionStage<SkillResult> {
        require(ticks in 0..1_728_000L) { "Delay must be between zero and 24 hours" }
        return state.track(engine.scheduler.on(frame.invocation.actor().entity(), ticks) { state.checkActive(); frame.checkActive(); SkillResult.success() })
    }
    override fun cast(key: SkillKey, parameters: Map<String, SkillValue>): CompletionStage<SkillResult> =
        platform.runNested(key, invocation().derive().parameters(parameters).build(), state, depth + 1, frame)
}
