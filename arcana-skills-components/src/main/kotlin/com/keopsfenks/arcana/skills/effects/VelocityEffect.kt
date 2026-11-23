package com.keopsfenks.arcana.skills.effects

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import org.bukkit.util.Vector

class VelocityEffect : SkillEffect("velocity") {
    override fun compile(arguments: SkillArguments, compiler: SkillCompiler): CompiledSkillEffect {
        ComponentArguments.keys(arguments, "x", "y", "z", "add", "target")
        val x = ComponentArguments.number(arguments, "x", compiler, -100.0, 100.0, 0.0)
        val y = ComponentArguments.number(arguments, "y", compiler, -100.0, 100.0, 0.0)
        val z = ComponentArguments.number(arguments, "z", compiler, -100.0, 100.0, 0.0)
        val add = arguments.bool("add", false)
        val target = ComponentArguments.target(arguments)
        return CompiledSkillEffect { execution -> execution.onTarget(target) { entity ->
            EffectValues.alive(entity) {
                val vector = Vector(x.evaluate(execution.context), y.evaluate(execution.context), z.evaluate(execution.context))
                entity.velocity = if (add) entity.velocity.clone().add(vector) else vector
            }
        } }
    }
}
