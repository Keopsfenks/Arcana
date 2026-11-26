package com.keopsfenks.arcana.skills.components.effects.flow

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.platform.execution.*
import java.util.UUID

class CycleMechanic : SkillMechanic(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("cycle", "Advance a revision-owned per-actor choice cursor", "choices")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val node = UUID.randomUUID()
        val choices = FlowSupport.choices(arguments).map { ComponentArguments.keys(it, "effects"); FlowSupport.effects(it, compilation) }
        return CompiledSkillMechanic { execution ->
            if (execution !is PlatformExecutionContext) FlowSupport.unsupported() else {
                val index = execution.services().require(FlowCursorStore::class.java).cycle(FlowSupport.cursor(execution, node), choices.size, execution.state::checkActive)
                execution.execute(choices[index])
            }
        }
    }
}
