package com.keopsfenks.arcana.skills.conditions

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.config.ComponentArguments

class HasTargetCondition : SkillCondition("has_target") {
    override fun compile(arguments: SkillArguments, compiler: SkillCompiler): CompiledSkillCondition {
        ComponentArguments.keys(arguments, "expected")
        val expected = arguments.bool("expected", true)
        return CompiledSkillCondition { (it.victim != null) == expected }
    }
}
