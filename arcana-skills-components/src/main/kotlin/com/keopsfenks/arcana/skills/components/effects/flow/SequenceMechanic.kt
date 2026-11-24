package com.keopsfenks.arcana.skills.components.effects.flow

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments

class SequenceMechanic : SkillMechanic(SkillDescriptor.of("sequence", "Execute child effects in order until one fails")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        ComponentArguments.keys(arguments, "effects")
        val effects = FlowSupport.effects(arguments, compilation)
        return CompiledSkillMechanic { it.execute(effects) }
    }
}
