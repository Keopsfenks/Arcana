package com.keopsfenks.arcana.skills.components.targeters

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.targets.TargetSelectionPolicy

class SelfTargeter : SkillTargeter(SkillDescriptor.of("self", "Select the invoking actor")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillTargeter {
        TargeterSupport.keys(arguments)
        val policy = TargetSelectionPolicy.compile(arguments, compilation)
        return CompiledSkillTargeter { TargeterSupport.actor(it, it.invocation().actor(), policy) }
    }
}
