package com.keopsfenks.arcana.skills.effects

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.config.ComponentArguments

class BranchEffect : SkillEffect("branch") {
    override fun compile(arguments: SkillArguments, compiler: SkillCompiler): CompiledSkillEffect {
        ComponentArguments.keys(arguments, "condition", "then", "else")
        val condition = compiler.condition(arguments.section("condition"))
        val yes = compiler.effects(arguments.list("then"), "${arguments.path}.then")
        val no = compiler.effects(arguments.list("else", emptyList<Any>()), "${arguments.path}.else")
        return CompiledSkillEffect { it.execute(if (condition.test(it.context)) yes else no) }
    }
}
