package com.keopsfenks.arcana.skills.components.mutators

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments

class ClampValueMutator : SkillMutator(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("clamp_value", "Clamp numeric child data to finite bounds", "key", "min", "max")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMutator {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val key = MutatorSupport.key(arguments)
        require(arguments.has("min") && arguments.has("max")) { "${arguments.path}: min and max are required" }
        if (arguments.raw("min") is Number && arguments.raw("max") is Number) require(arguments.number("min", 0.0) <= arguments.number("max", 0.0)) { "${arguments.path}: min exceeds max" }
        val min = MutatorSupport.number(arguments, compilation, "min", 0); val max = MutatorSupport.number(arguments, compilation, "max", 0)
        return CompiledSkillMutator { input -> val low = min.evaluate(input); val high = max.evaluate(input)
            require(low <= high) { "${arguments.path}: min exceeds max" }
            input.derive().data(mapOf(key to SkillValue.of(MutatorSupport.numeric(input, key).coerceIn(low, high)))).build()
        }
    }
}
