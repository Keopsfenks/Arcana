package com.keopsfenks.arcana.skills.effects

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.config.ComponentArguments

class SequenceEffect : SkillEffect("sequence") {
    override fun compile(arguments: SkillArguments, compiler: SkillCompiler): CompiledSkillEffect {
        ComponentArguments.keys(arguments, "effects")
        val children = compiler.effects(arguments.list("effects"), "${arguments.path}.effects")
        return CompiledSkillEffect { it.execute(children) }
    }
}
