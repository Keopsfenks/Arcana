package com.keopsfenks.arcana.skills.conditions

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.config.ComponentArguments

class PermissionCondition : SkillCondition("permission") {
    override fun compile(arguments: SkillArguments, compiler: SkillCompiler): CompiledSkillCondition {
        ComponentArguments.keys(arguments, "permission")
        val permission = ComponentArguments.nonEmpty(arguments, "permission")
        return CompiledSkillCondition { it.caster.hasPermission(permission) }
    }
}
