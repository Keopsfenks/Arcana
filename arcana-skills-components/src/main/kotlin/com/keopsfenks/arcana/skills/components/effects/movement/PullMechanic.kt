package com.keopsfenks.arcana.skills.components.effects.movement
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.effects.EffectValues

class PullMechanic : SkillMechanic(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("pull", "Pull a living actor toward the captured caster position", "strength", "target")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor()); val recipient = NativeActions.recipient(arguments)
        val strength = NativeActions.number(arguments, compilation, "strength", 1, 0.0, 16.0)
        return CompiledSkillMechanic { context -> NativeActions.run(context, recipient) { entity -> EffectValues.alive(entity) { entity.velocity = MovementActions.impulse(context.invocation(), entity, strength.evaluate(context.invocation()), true) } } }
    }
}
