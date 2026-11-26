package com.keopsfenks.arcana.skills.components.targeters

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.targets.TargetSelectionPolicy

class OriginTargeter : SkillTargeter(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("origin", "Select the immutable invocation origin", *com.keopsfenks.arcana.skills.platform.targets.TargetSelectionPolicy.OPTIONS)) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillTargeter {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val policy = TargetSelectionPolicy.compile(arguments, compilation)
        return CompiledSkillTargeter { context -> TargeterSupport.locations(context, context.invocation().origin().orElseThrow { IllegalArgumentException("Origin snapshot is unavailable") }, policy) }
    }
}
