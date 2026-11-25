package com.keopsfenks.arcana.skills.components.effects.movement
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.effects.EffectValues
import com.keopsfenks.arcana.skills.platform.projectiles.ProjectileMotion

class VelocityMechanic : SkillMechanic(SkillDescriptor.of("velocity", "Set bounded living-actor velocity, optionally normalized")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        ComponentArguments.keys(arguments, "x", "y", "z", "normalize", "speed", "target")
        val recipient = NativeActions.recipient(arguments); val x = NativeActions.number(arguments, compilation, "x", 0, -16.0, 16.0)
        val y = NativeActions.number(arguments, compilation, "y", 0, -16.0, 16.0); val z = NativeActions.number(arguments, compilation, "z", 0, -16.0, 16.0)
        val normalize = arguments.bool("normalize", false); val speed = NativeActions.number(arguments, compilation, "speed", 1, 0.0, 16.0)
        return CompiledSkillMechanic { context -> NativeActions.run(context, recipient) { entity -> EffectValues.alive(entity) {
            val input = context.invocation(); val vector = SkillVector(x.evaluate(input), y.evaluate(input), z.evaluate(input))
            entity.velocity = MovementActions.bounded(if (normalize) ProjectileMotion.scale(ProjectileMotion.unit(vector), speed.evaluate(input)) else vector)
        } } }
    }
}
