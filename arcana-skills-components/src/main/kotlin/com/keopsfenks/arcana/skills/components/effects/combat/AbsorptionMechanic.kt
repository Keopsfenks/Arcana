package com.keopsfenks.arcana.skills.components.effects.combat
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.NativeActions
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.effects.EffectValues

class AbsorptionMechanic : SkillMechanic(SkillDescriptor.of("absorption", "Add absorption up to a configured finite maximum")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        ComponentArguments.keys(arguments, "amount", "maximum", "target"); val recipient = NativeActions.recipient(arguments)
        val amount = NativeActions.number(arguments, compilation, "amount", 1, 0.0, 1_000_000.0); val maximum = NativeActions.number(arguments, compilation, "maximum", 40, 0.0, 1_000_000.0)
        return CompiledSkillMechanic { context -> NativeActions.run(context, recipient) { entity -> EffectValues.alive(entity) { val input = context.invocation(); entity.absorptionAmount = (entity.absorptionAmount+amount.evaluate(input)).coerceIn(0.0, maximum.evaluate(input)) } } }
    }
}
