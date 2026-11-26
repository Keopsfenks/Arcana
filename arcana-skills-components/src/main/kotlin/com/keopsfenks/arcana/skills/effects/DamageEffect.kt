package com.keopsfenks.arcana.skills.effects

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.execution.SkillDamageScope

/** Uses the server's normal damage rules; no foreign-region attacker state is accessed. */
class DamageEffect : SkillEffect("damage") {
    override fun compile(arguments: SkillArguments, compiler: SkillCompiler): CompiledSkillEffect {
        ComponentArguments.keys(arguments, "amount", "target")
        val amount = ComponentArguments.number(arguments, "amount", compiler, 0.0, 1_000_000.0)
        val target = ComponentArguments.target(arguments)
        return CompiledSkillEffect { execution -> execution.onTarget(target) { entity ->
            EffectValues.alive(entity) { SkillDamageScope.during { entity.damage(amount.evaluate(execution.context)) } }
        } }
    }
}
