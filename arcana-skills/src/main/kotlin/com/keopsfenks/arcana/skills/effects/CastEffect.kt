package com.keopsfenks.arcana.skills.effects

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.config.ComponentArguments

class CastEffect : SkillEffect("cast") {
    override fun compile(arguments: SkillArguments, compiler: SkillCompiler): CompiledSkillEffect {
        ComponentArguments.keys(arguments, "skill")
        val id = arguments.string("skill")
        compiler.referenceSkill(id, "${arguments.path}.skill")
        return CompiledSkillEffect { it.cast(id) }
    }
}
