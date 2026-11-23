package com.keopsfenks.arcana.skills.components.targeters

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.targets.TargetSelectionPolicy

class LocationTargeter : SkillTargeter(SkillDescriptor.of("location", "Select the current invocation location")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillTargeter {
        TargeterSupport.keys(arguments)
        val policy = TargetSelectionPolicy.compile(arguments, compilation)
        return CompiledSkillTargeter { context -> TargeterSupport.locations(context, context.invocation().location().orElseThrow { IllegalArgumentException("Location snapshot is unavailable") }, policy) }
    }
}
