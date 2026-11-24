package com.keopsfenks.arcana.skills.components.mutators

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments

class MultiplyValueMutator : SkillMutator(SkillDescriptor.of("multiply_value", "Multiply numeric child data without changing its parent")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMutator {
        ComponentArguments.keys(arguments, "key", "factor")
        val key = MutatorSupport.key(arguments); val factor = MutatorSupport.number(arguments, compilation, "factor", 1)
        return CompiledSkillMutator { input -> input.derive().data(mapOf(key to SkillValue.of(MutatorSupport.numeric(input, key) * factor.evaluate(input)))).build() }
    }
}
