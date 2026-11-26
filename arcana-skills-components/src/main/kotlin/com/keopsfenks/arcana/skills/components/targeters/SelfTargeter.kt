package com.keopsfenks.arcana.skills.components.targeters

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.targets.TargetSelectionPolicy

class SelfTargeter : SkillTargeter(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("self", "Select the invoking actor", *com.keopsfenks.arcana.skills.platform.targets.TargetSelectionPolicy.OPTIONS)) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillTargeter {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val policy = TargetSelectionPolicy.compile(arguments, compilation)
        return CompiledSkillTargeter { TargeterSupport.actor(it, it.invocation().actor(), policy) }
    }
}
