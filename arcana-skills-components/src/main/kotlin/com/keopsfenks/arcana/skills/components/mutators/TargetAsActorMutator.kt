package com.keopsfenks.arcana.skills.components.mutators

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.platform.execution.MissingSkillTargetException

class TargetAsActorMutator : SkillMutator(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("target_as_actor", "Use the selected target as the child actor")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMutator {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        return CompiledSkillMutator { input -> input.derive().actor(input.target().orElseThrow { MissingSkillTargetException("Actor replacement requires a target") }).build() }
    }
}
