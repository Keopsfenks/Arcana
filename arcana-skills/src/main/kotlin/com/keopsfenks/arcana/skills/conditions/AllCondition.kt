package com.keopsfenks.arcana.skills.conditions

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.config.ComponentArguments

class AllCondition : SkillCondition("all") {
    override fun compile(arguments: SkillArguments, compiler: SkillCompiler): CompiledSkillCondition {
        ComponentArguments.keys(arguments, "conditions")
        val list = arguments.list("conditions")
        require(list.isNotEmpty()) { "${arguments.path}.conditions: must not be empty" }
        return compiler.conditions(list, "${arguments.path}.conditions")
    }
}
