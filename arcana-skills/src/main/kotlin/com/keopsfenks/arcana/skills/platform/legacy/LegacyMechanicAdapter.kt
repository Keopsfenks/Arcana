package com.keopsfenks.arcana.skills.platform.legacy

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.execution.InvocationSnapshots
import com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionContext
import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import java.util.concurrent.*
import java.util.function.Function

/** Old Player closures run on their caster owner and borrow native cancellation/steps/depth. */
class LegacyMechanicAdapter(private val effect: CompiledSkillEffect) : CompiledSkillMechanic {
    override fun execute(execution: SkillExecutionContext): CompletionStage<SkillResult> {
        val invocation = execution.invocation()
        val context = try { LegacyInvocationAdapter.toLegacy(invocation) }
        catch (error: LegacyActorException) { return CompletableFuture.completedFuture(error.result) }
        if (execution !is PlatformExecutionContext) return CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.UNSUPPORTED_CONTEXT, "Legacy adapters require an Arcana execution context"))
        val adapter = object : SkillExecution {
            override fun getContext() = context
            private fun actor(target: SkillTarget) = if (target == SkillTarget.CASTER) invocation.actor() else invocation.target().orElse(null)
            override fun onTarget(target: SkillTarget, action: Function<LivingEntity, SkillResult>): CompletionStage<SkillResult> =
                actor(target)?.let { execution.on(it, action) } ?: missing()
            override fun atTarget(target: SkillTarget, action: Function<Location, SkillResult>): CompletionStage<SkillResult> {
                val selected = actor(target) ?: return missing()
                return execution.track(execution.engine.scheduler.on(selected.entity()) {
                    execution.state.checkActive()
                    if (!InvocationSnapshots.valid(selected.entity())) null else selected.entity().location.let { location ->
                        SkillLocation(requireNotNull(location.world).uid, location.x, location.y, location.z, location.yaw, location.pitch)
                    }
                }).thenCompose { if (it == null) missing() else execution.at(it, action).toCompletableFuture() }
            }
            override fun global(action: Runnable): CompletionStage<SkillResult> = execution.track(execution.engine.scheduler.global {
                execution.state.checkActive(); execution.frame.checkActive(); action.run(); SkillResult.success()
            })
            override fun execute(effects: List<CompiledSkillEffect>): CompletionStage<SkillResult> = execution.execute(effects.map(::LegacyMechanicAdapter))
            override fun delay(ticks: Long) = execution.delay(ticks)
            override fun cast(skillId: String): CompletionStage<SkillResult> = execution.cast(SkillKey.parse(skillId, execution.definition.key.namespace()), emptyMap())
            override fun format(text: String) = text.replace("{player}", context.getText("caster_name")).replace("{target}", context.getText("target_name")).replace("{skill}", execution.definition.key.toString())
            private fun missing() = CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.NO_TARGET, "No living target was provided"))
        }
        return execution.track(execution.engine.scheduler.on(invocation.actor().entity()) {
            execution.state.checkActive(); execution.frame.checkActive()
            if (!InvocationSnapshots.valid(invocation.actor().entity())) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.CANCELLED, "Actor retired"))
            else effect.execute(adapter)
        }).thenCompose { it.toCompletableFuture() }
    }
}
