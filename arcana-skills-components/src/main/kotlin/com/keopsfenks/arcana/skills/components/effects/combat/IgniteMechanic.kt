package com.keopsfenks.arcana.skills.components.effects.combat
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.NativeActions
import com.keopsfenks.arcana.skills.components.auras.AuraSupport
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.effects.EffectValues

class IgniteMechanic : SkillMechanic(SkillDescriptor.of("ignite", "Extend native burning for a finite duration")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        ComponentArguments.keys(arguments, "duration", "target"); val recipient = NativeActions.recipient(arguments)
        val duration = AuraSupport.duration(arguments, compilation, "duration", "2s")
        return CompiledSkillMechanic { context -> NativeActions.run(context, recipient) { entity -> EffectValues.alive(entity) { entity.fireTicks = maxOf(entity.fireTicks, duration.evaluate(context.invocation()).toInt()) } } }
    }
}
