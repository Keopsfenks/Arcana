package com.keopsfenks.arcana.skills.components.effects.flow

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments

class CastMechanic : SkillMechanic(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("cast", "Call a namespaced skill with explicit immutable parameters", "skill", "parameters")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val key = SkillKey.parse(arguments.string("skill"), compilation.namespace())
        compilation.referenceSkill(key, "${arguments.path}.skill")
        val parameters = if (!arguments.has("parameters")) emptyMap() else arguments.section("parameters").values.mapValues { (name, raw) ->
            SkillKey("arcana", name)
            FlowSupport.parameter(raw, compilation, "${arguments.path}.parameters.$name")
        }
        return CompiledSkillMechanic { execution -> val input = execution.invocation(); execution.cast(key, parameters.mapValues { it.value(input) }) }
    }
}
