package com.keopsfenks.arcana.skills.components.targeters

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.targets.TargetSelectionPolicy

class LocationTargeter : SkillTargeter(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("location", "Select the current invocation location", *com.keopsfenks.arcana.skills.platform.targets.TargetSelectionPolicy.OPTIONS)) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillTargeter {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val policy = TargetSelectionPolicy.compile(arguments, compilation)
        return CompiledSkillTargeter { context -> TargeterSupport.locations(context, context.invocation().location().orElseThrow { IllegalArgumentException("Location snapshot is unavailable") }, policy) }
    }
}
