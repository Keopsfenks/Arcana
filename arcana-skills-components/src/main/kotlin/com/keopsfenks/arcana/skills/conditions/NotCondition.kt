package com.keopsfenks.arcana.skills.conditions

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.config.ComponentArguments

class NotCondition : SkillCondition("not") {
    override fun compile(arguments: SkillArguments, compiler: SkillCompiler): CompiledSkillCondition {
        ComponentArguments.keys(arguments, "condition")
        val compiled = compiler.condition(arguments.section("condition"))
        return CompiledSkillCondition { !compiled.test(it) }
    }
}
