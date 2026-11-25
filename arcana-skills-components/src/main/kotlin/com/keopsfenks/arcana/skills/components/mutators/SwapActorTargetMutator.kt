package com.keopsfenks.arcana.skills.components.mutators

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.platform.execution.MissingSkillTargetException

class SwapActorTargetMutator : SkillMutator(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("swap_actor_target", "Swap actor and current entity target in a child invocation")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMutator {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        return CompiledSkillMutator { input -> input.derive().actor(input.target().orElseThrow { MissingSkillTargetException("Swap requires a target") }).target(input.actor()).build() }
    }
}
