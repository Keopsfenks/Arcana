package com.keopsfenks.arcana.skills.components.effects.movement
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.effects.EffectValues

class KnockbackMechanic : SkillMechanic(SkillDescriptor.of("knockback", "Push a living actor away from the captured caster position")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        ComponentArguments.keys(arguments, "strength", "target"); val recipient = NativeActions.recipient(arguments)
        val strength = NativeActions.number(arguments, compilation, "strength", 1, 0.0, 16.0)
        return CompiledSkillMechanic { context -> NativeActions.run(context, recipient) { entity -> EffectValues.alive(entity) { entity.velocity = MovementActions.impulse(context.invocation(), entity, strength.evaluate(context.invocation()), false) } } }
    }
}
