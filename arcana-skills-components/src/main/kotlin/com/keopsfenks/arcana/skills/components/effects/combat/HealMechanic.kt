package com.keopsfenks.arcana.skills.components.effects.combat
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.NativeActions
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.effects.EffectValues

class HealMechanic : SkillMechanic(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("heal", "Heal a living actor within its native maximum health", "amount", "target")) {
    @Suppress("DEPRECATION") override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val recipient = NativeActions.recipient(arguments); val amount = NativeActions.number(arguments, compilation, "amount", 1, 0.0, 1_000_000.0)
        return CompiledSkillMechanic { context -> NativeActions.run(context, recipient) { entity -> EffectValues.alive(entity) { entity.health = (entity.health+amount.evaluate(context.invocation())).coerceIn(0.0, entity.maxHealth) } } }
    }
}
