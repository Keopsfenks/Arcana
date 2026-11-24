package com.keopsfenks.arcana.skills.components.mutators

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.platform.execution.MissingSkillTargetException

class TargetAsActorMutator : SkillMutator(SkillDescriptor.of("target_as_actor", "Use the selected target as the child actor")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMutator {
        ComponentArguments.keys(arguments)
        return CompiledSkillMutator { input -> input.derive().actor(input.target().orElseThrow { MissingSkillTargetException("Actor replacement requires a target") }).build() }
    }
}
