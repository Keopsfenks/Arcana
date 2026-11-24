package com.keopsfenks.arcana.skills.components.effects.flow

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.platform.compile.NodeOptions

class WeightedRandomMechanic : SkillMechanic(SkillDescriptor.of("weighted_random", "Choose one child sequence with positive finite weights")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        ComponentArguments.keys(arguments, "choices")
        val choices = FlowSupport.choices(arguments).map { args ->
            ComponentArguments.keys(args, "weight", "effects")
            NodeOptions.number(args, compilation, "weight", 1, Double.MIN_VALUE, Double.MAX_VALUE) to FlowSupport.effects(args, compilation)
        }
        return CompiledSkillMechanic { execution ->
            val weights = choices.map { it.first.evaluate(execution.invocation()) }
            val total = weights.sum().also { require(it.isFinite() && it > 0) { "${arguments.path}: weight sum must be finite and positive" } }
            val roll = FlowSupport.roll(execution) * total
            var cumulative = 0.0
            var selected = choices.lastIndex
            for (index in weights.indices) { cumulative += weights[index]; if (roll < cumulative) { selected = index; break } }
            execution.execute(choices[selected].second)
        }
    }
}
