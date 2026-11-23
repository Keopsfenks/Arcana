package com.keopsfenks.arcana.skills.conditions

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.config.ComponentArguments

class FoodAboveCondition : SkillCondition("food_above") {
    override fun compile(arguments: SkillArguments, compiler: SkillCompiler): CompiledSkillCondition {
        ComponentArguments.keys(arguments, "amount")
        val amount = ComponentArguments.number(arguments, "amount", compiler, 0.0, 20.0)
        return CompiledSkillCondition { it.getNumber("caster_food") > amount.evaluate(it) }
    }
}
