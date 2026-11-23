package com.keopsfenks.arcana.skills.conditions

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.config.ComponentArguments

class ExpressionCondition : SkillCondition("expression") {
    override fun compile(arguments: SkillArguments, compiler: SkillCompiler): CompiledSkillCondition {
        ComponentArguments.keys(arguments, "expression")
        val number = compiler.number(arguments.raw("expression"), "${arguments.path}.expression")
        return CompiledSkillCondition { number.evaluate(it) != 0.0 }
    }
}
