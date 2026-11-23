package com.keopsfenks.arcana.skills.effects

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import org.bukkit.entity.Player

class FeedEffect : SkillEffect("feed") {
    override fun compile(arguments: SkillArguments, compiler: SkillCompiler): CompiledSkillEffect {
        ComponentArguments.keys(arguments, "amount", "saturation", "target")
        val amount = ComponentArguments.number(arguments, "amount", compiler, -20.0, 20.0, integer = true)
        val saturation = ComponentArguments.number(arguments, "saturation", compiler, -20.0, 20.0, 0.0)
        val target = ComponentArguments.target(arguments)
        return CompiledSkillEffect { execution -> execution.onTarget(target) { entity ->
            if (entity !is Player) SkillResult.of(SkillResult.Status.NO_TARGET, "Feed target must be a player")
            else EffectValues.alive(entity) {
                entity.foodLevel = (entity.foodLevel + amount.evaluate(execution.context).toInt()).coerceIn(0, 20)
                entity.saturation = (entity.saturation + saturation.evaluate(execution.context).toFloat()).coerceIn(0f, entity.foodLevel.toFloat())
            }
        } }
    }
}
