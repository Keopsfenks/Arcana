package com.keopsfenks.arcana.skills.components.effects.flow

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.platform.execution.*
import java.util.UUID

class CycleMechanic : SkillMechanic(SkillDescriptor.of("cycle", "Advance a revision-owned per-actor choice cursor")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        ComponentArguments.keys(arguments, "choices")
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
