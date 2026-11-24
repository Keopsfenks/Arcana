package com.keopsfenks.arcana.skills.components.mutators

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments

class SetValueMutator : SkillMutator(SkillDescriptor.of("set_value", "Set child data to a typed literal or numeric expression")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMutator {
        ComponentArguments.keys(arguments, "key", "value", "expression")
        val key = MutatorSupport.key(arguments)
        require(arguments.has("value") != arguments.has("expression")) { "${arguments.path}: supply value or expression" }
        val literal = if (arguments.has("value")) SkillValue.of(arguments.raw("value")) else null
        val expression = if (literal == null) compilation.number(arguments.raw("expression"), "${arguments.path}.expression") else null
        return CompiledSkillMutator { input -> input.derive().data(mapOf(key to (literal ?: SkillValue.of(requireNotNull(expression).evaluate(input))))).build() }
    }
}
