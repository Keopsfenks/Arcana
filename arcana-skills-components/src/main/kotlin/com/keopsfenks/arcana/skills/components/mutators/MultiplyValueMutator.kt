package com.keopsfenks.arcana.skills.components.mutators

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments

class MultiplyValueMutator : SkillMutator(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("multiply_value", "Multiply numeric child data without changing its parent", "key", "factor")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMutator {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val key = MutatorSupport.key(arguments); val factor = MutatorSupport.number(arguments, compilation, "factor", 1)
        return CompiledSkillMutator { input -> input.derive().data(mapOf(key to SkillValue.of(MutatorSupport.numeric(input, key) * factor.evaluate(input)))).build() }
    }
}
