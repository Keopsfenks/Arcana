package com.keopsfenks.arcana.skills.components.effects.combat
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.NativeActions
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.effects.EffectValues

class ExtinguishMechanic : SkillMechanic(SkillDescriptor.of("extinguish", "Clear a living actor's native burning")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        ComponentArguments.keys(arguments, "target"); val recipient = NativeActions.recipient(arguments)
        return CompiledSkillMechanic { context -> NativeActions.run(context, recipient) { entity -> EffectValues.alive(entity) { entity.fireTicks = 0 } } }
    }
}
