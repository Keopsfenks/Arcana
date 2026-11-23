package com.keopsfenks.arcana.skills.conditions

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.config.ComponentArguments

class WorldCondition : SkillCondition("world") {
    override fun compile(arguments: SkillArguments, compiler: SkillCompiler): CompiledSkillCondition {
        ComponentArguments.keys(arguments, "name")
        val name = ComponentArguments.nonEmpty(arguments, "name")
        return CompiledSkillCondition { it.getText("world") == name }
    }
}
