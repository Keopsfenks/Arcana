package com.keopsfenks.arcana.skills.components.targeters

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.targets.TargetSelectionPolicy

class TriggerTargeter : SkillTargeter(SkillDescriptor.of("trigger", "Select the entity that caused the trigger")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillTargeter {
        TargeterSupport.keys(arguments)
        val policy = TargetSelectionPolicy.compile(arguments, compilation)
        return CompiledSkillTargeter { TargeterSupport.actor(it, it.invocation().trigger().orElse(null), policy) }
    }
}
