package com.keopsfenks.arcana.skills.execution

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.DefaultSkillEngine
import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutionException
import java.util.function.Function

class SkillExecutionImpl(
    private val engine: DefaultSkillEngine,
    private val state: ExecutionState,
    private val context: SkillContext,
    private val depth: Int
) : SkillExecution {
    override fun getContext() = context

    override fun onTarget(target: SkillTarget, action: Function<LivingEntity, SkillResult>): CompletionStage<SkillResult> {
        val entity = if (target == SkillTarget.CASTER) context.caster else context.victim
        if (entity == null) return missing()
        return victimRetirement(target, state.track(engine.scheduler.on(entity) {
            state.checkActive()
            if (if (entity is Player) !entity.isOnline else !entity.isValid) SkillResult.of(SkillResult.Status.NO_TARGET, "Target is unavailable")
            else action.apply(entity)
        }))
    }

    override fun atTarget(target: SkillTarget, action: Function<Location, SkillResult>): CompletionStage<SkillResult> {
        val entity = if (target == SkillTarget.CASTER) context.caster else context.victim
        if (entity == null) return missing()
        val dispatched = state.track(engine.scheduler.on(entity) {
            state.checkActive()
            if (if (entity is Player) !entity.isOnline else !entity.isValid) null else entity.location.clone()
        }).thenCompose { location ->
            if (location == null) missing()
            else state.track(engine.scheduler.at(location) { state.checkActive(); action.apply(location) })
        }
        return victimRetirement(target, dispatched)
    }

    override fun global(action: Runnable): CompletionStage<SkillResult> = state.track(engine.scheduler.global {
        state.checkActive()
        action.run()
        SkillResult.success()
    })

    override fun execute(effects: List<CompiledSkillEffect>): CompletionStage<SkillResult> {
        var result = CompletableFuture.completedFuture(SkillResult.success())
        for (effect in effects) {
            result = result.thenCompose { previous ->
                if (!previous.isSuccess) CompletableFuture.completedFuture(previous)
                else state.track(engine.scheduler.on(context.caster) {
                    state.step()
                    effect.execute(this)
                }).thenCompose { it.toCompletableFuture() }
            }
        }
        return result
    }

    override fun delay(ticks: Long): CompletionStage<SkillResult> {
        require(ticks in 0..1_728_000L) { "Delay must be between zero and 24 hours" }
        return state.track(engine.scheduler.on(context.caster, ticks) { state.checkActive(); SkillResult.success() })
    }

    override fun cast(skillId: String): CompletionStage<SkillResult> {
        val parent = context.getText("skill")
        val resolved = if (':' !in skillId && ':' in parent) parent.substringBefore(':') + ":" + skillId else skillId
        return engine.runNested(resolved, context, state, depth + 1)
    }
    override fun format(text: String) = text.replace("{player}", context.getText("caster_name"))
        .replace("{target}", context.getText("target_name")).replace("{skill}", context.getText("skill"))

    private fun missing() = CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.NO_TARGET, "No living victim was provided"))

    private fun victimRetirement(target: SkillTarget, result: CompletableFuture<SkillResult>): CompletableFuture<SkillResult> =
        result.handle { value, error ->
            state.checkActive()
            if (error == null) value
            else {
                var cause = error
                while ((cause is CompletionException || cause is ExecutionException) && cause.cause != null) cause = cause.cause!!
                if (target == SkillTarget.VICTIM && cause is CancellationException) SkillResult.of(SkillResult.Status.NO_TARGET, "Victim is unavailable")
                else throw CompletionException(cause)
            }
        }
}
