package com.keopsfenks.arcana.skills.components.effects.variables

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments

class AddVariableMechanic : SkillMechanic(SkillDescriptor.of("add_variable", "Atomically add a finite amount to a numeric variable")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        ComponentArguments.keys(arguments, "scope", "key", "amount")
        val scope = VariableArguments.scope(arguments)
        val key = SkillKey.parse(arguments.string("key"), compilation.namespace())
        val amount = compilation.number(arguments.raw("amount", 1), "${arguments.path}.amount")
        return CompiledSkillMechanic { execution -> VariableArguments.run(execution) {
            it.add(scope, key, amount.evaluate(execution.invocation()))
        } }
    }
}
