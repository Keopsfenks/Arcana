package com.keopsfenks.arcana.skills.components.mutators

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.platform.execution.MissingSkillTargetException

class TriggerAsTargetMutator : SkillMutator(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("trigger_as_target", "Use the triggering entity as the child target")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMutator {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        return CompiledSkillMutator { input -> input.derive().target(input.trigger().orElseThrow { MissingSkillTargetException("Trigger entity is unavailable") }).build() }
    }
}
