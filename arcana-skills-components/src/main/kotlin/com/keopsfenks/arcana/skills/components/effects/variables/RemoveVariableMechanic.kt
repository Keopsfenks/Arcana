package com.keopsfenks.arcana.skills.components.effects.variables

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments

class RemoveVariableMechanic : SkillMechanic(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("remove_variable", "Remove only the selected variable", "scope", "key")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val scope = VariableArguments.scope(arguments)
        val key = SkillKey.parse(arguments.string("key"), compilation.namespace())
        return CompiledSkillMechanic { execution -> VariableArguments.run(execution) { it.remove(scope, key) } }
    }
}
