package com.keopsfenks.arcana.skills.components.effects.variables

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.config.SkillArgumentsReader
import com.keopsfenks.arcana.skills.platform.values.ParameterBindings
import java.time.Duration

class SetVariableMechanic : SkillMechanic(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("set_variable", "Set one typed variable in an explicit scope", "scope", "key", "value", "value-type", "expression", "ttl")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val scope = VariableArguments.scope(arguments)
        val key = SkillKey.parse(arguments.string("key"), compilation.namespace())
        require(arguments.has("value") != arguments.has("expression")) { "${arguments.path}: supply either value or expression" }
        val expression = if (arguments.has("expression")) compilation.number(arguments.raw("expression"), "${arguments.path}.expression") else null
        val literal = if (arguments.has("value")) {
            if (arguments.has("value-type")) ParameterBindings.literal(ParameterBindings.kind(arguments.string("value-type"), "${arguments.path}.value-type"),
                arguments.raw("value"), compilation.namespace(), "${arguments.path}.value") else SkillValue.of(arguments.raw("value"))
        } else null
        val ttl = Duration.ofNanos(SkillArgumentsReader.durationNanos(arguments.raw("ttl", "0s"), "${arguments.path}.ttl"))
        return CompiledSkillMechanic { execution -> VariableArguments.run(execution) { variables ->
            variables.set(scope, key, literal ?: SkillValue.of(requireNotNull(expression).evaluate(execution.invocation())), ttl)
        } }
    }
}
