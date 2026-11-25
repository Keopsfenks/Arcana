package com.keopsfenks.arcana.skills.components.effects.flow

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments

class SequenceMechanic : SkillMechanic(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("sequence", "Execute child effects in order until one fails", "effects")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val effects = FlowSupport.effects(arguments, compilation)
        return CompiledSkillMechanic { it.execute(effects) }
    }
}
