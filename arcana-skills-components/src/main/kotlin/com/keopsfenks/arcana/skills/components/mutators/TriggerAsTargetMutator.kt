package com.keopsfenks.arcana.skills.components.mutators

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.platform.execution.MissingSkillTargetException

class TriggerAsTargetMutator : SkillMutator(SkillDescriptor.of("trigger_as_target", "Use the triggering entity as the child target")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMutator {
        ComponentArguments.keys(arguments)
        return CompiledSkillMutator { input -> input.derive().target(input.trigger().orElseThrow { MissingSkillTargetException("Trigger entity is unavailable") }).build() }
    }
}
