package com.keopsfenks.arcana.skills.conditions

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.config.ComponentArguments

class HealthBelowCondition : SkillCondition("health_below") {
    override fun compile(arguments: SkillArguments, compiler: SkillCompiler): CompiledSkillCondition {
        ComponentArguments.keys(arguments, "ratio")
        val ratio = ComponentArguments.number(arguments, "ratio", compiler, 0.0, 1.0)
        return CompiledSkillCondition { it.getNumber("caster_max_health") > 0 && it.getNumber("caster_health") / it.getNumber("caster_max_health") < ratio.evaluate(it) }
    }
}
