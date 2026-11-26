package com.keopsfenks.arcana.skills.components.effects.status
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.NativeActions
import com.keopsfenks.arcana.skills.components.auras.AuraSupport
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.effects.EffectValues
import com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionContext
import com.keopsfenks.arcana.skills.platform.managed.ManagedPulse
import java.util.concurrent.CompletableFuture

class FreezeMechanic : SkillMechanic(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("freeze", "Refresh native finite freeze ticks without changing another plugin's freeze lock", "duration", "target")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor()); val recipient = NativeActions.recipient(arguments)
        val duration = AuraSupport.duration(arguments, compilation, "duration", "1s")
        return CompiledSkillMechanic { context ->
            val input = context.invocation(); val actor = recipient(input)
            if (actor == null) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.NO_TARGET, "Freeze target missing"))
            else if (context !is PlatformExecutionContext) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.UNSUPPORTED_CONTEXT, "Freeze requires Arcana execution"))
            else {
                val pulse = CompiledSkillMechanic { current -> current.on(actor) { entity -> EffectValues.alive(entity) {
                    if (!entity.isFreezeTickingLocked) entity.freezeTicks = maxOf(entity.freezeTicks, entity.maxFreezeTicks)
                } } }
                ManagedPulse.start(context, duration.evaluate(input).toLong(), 1, pulse = listOf(pulse), recipient = actor).thenApply { SkillResult.success() }
            }
        }
    }
}
