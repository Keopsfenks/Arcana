package com.keopsfenks.arcana.skills.effects

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.config.SkillArgumentsReader

class IgniteEffect : SkillEffect("ignite") {
    override fun compile(arguments: SkillArguments, compiler: SkillCompiler): CompiledSkillEffect {
        ComponentArguments.keys(arguments, "duration", "target")
        val ticks = SkillArgumentsReader.ticks(arguments.raw("duration"), "${arguments.path}.duration").toInt()
        val target = ComponentArguments.target(arguments)
        return CompiledSkillEffect { execution -> execution.onTarget(target) { entity ->
            EffectValues.alive(entity) { entity.fireTicks = ticks }
        } }
    }
}
