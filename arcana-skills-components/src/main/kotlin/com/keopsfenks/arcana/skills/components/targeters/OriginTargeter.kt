package com.keopsfenks.arcana.skills.components.targeters

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.targets.TargetSelectionPolicy

class OriginTargeter : SkillTargeter(SkillDescriptor.of("origin", "Select the immutable invocation origin")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillTargeter {
        TargeterSupport.keys(arguments)
        val policy = TargetSelectionPolicy.compile(arguments, compilation)
        return CompiledSkillTargeter { context -> TargeterSupport.locations(context, context.invocation().origin().orElseThrow { IllegalArgumentException("Origin snapshot is unavailable") }, policy) }
    }
}
