package com.keopsfenks.arcana.skills.effects

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import org.bukkit.potion.PotionEffectType

class RemovePotionEffect(private val resolve: (String) -> PotionEffectType? = PotionValues::resolve) : SkillEffect("remove_potion") {
    override fun compile(arguments: SkillArguments, compiler: SkillCompiler): CompiledSkillEffect {
        ComponentArguments.keys(arguments, "potion", "target")
        val target = ComponentArguments.target(arguments)
        val type = resolve(arguments.string("potion")) ?: throw arguments.error("potion", "unknown potion type")
        return CompiledSkillEffect { execution -> execution.onTarget(target) { entity ->
            EffectValues.alive(entity) { entity.removePotionEffect(type) }
        } }
    }
}
