package com.keopsfenks.arcana.skills.conditions

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.config.ComponentArguments

class AnyCondition : SkillCondition("any") {
    override fun compile(arguments: SkillArguments, compiler: SkillCompiler): CompiledSkillCondition {
        ComponentArguments.keys(arguments, "conditions")
        val list = arguments.list("conditions")
        require(list.isNotEmpty()) { "${arguments.path}.conditions: must not be empty" }
        val compiled = list.mapIndexed { index, value -> compiler.condition(arguments.child("conditions[$index]", value)) }
        return CompiledSkillCondition { context -> compiled.any { it.test(context) } }
    }
}
