package com.keopsfenks.arcana.skills.components.effects.flow

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.platform.execution.ParallelScope

class ParallelMechanic : SkillMechanic(SkillDescriptor.of("parallel", "Execute up to 16 isolated branches with ordered outcomes")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        ComponentArguments.keys(arguments, "branches", "fail-fast")
        val raw = arguments.list("branches")
        require(raw.isNotEmpty() && raw.size <= 16) { "${arguments.path}.branches: expected 1..16 branches" }
        val branches = raw.mapIndexed { index, value ->
            val args = arguments.child("branches[$index]", value)
            ComponentArguments.keys(args, "effects")
            FlowSupport.effects(args, compilation)
        }
        val failFast = arguments.bool("fail-fast", true)
        return CompiledSkillMechanic { it.services().require(ParallelScope::class.java).execute(branches, failFast).thenApply(ParallelScope::aggregate) }
    }
}
