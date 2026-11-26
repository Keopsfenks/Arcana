package com.keopsfenks.arcana.skills.components.effects.movement
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.effects.EffectValues

class LaunchMechanic : SkillMechanic(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("launch", "Replace vertical velocity with bounded upward strength", "strength", "target")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor()); val recipient = NativeActions.recipient(arguments)
        val strength = NativeActions.number(arguments, compilation, "strength", 1, 0.0, 16.0)
        return CompiledSkillMechanic { context -> NativeActions.run(context, recipient) { entity -> EffectValues.alive(entity) { val previous = entity.velocity; entity.velocity = MovementActions.bounded(SkillVector(previous.x, strength.evaluate(context.invocation()), previous.z)) } } }
    }
}
