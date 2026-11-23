package com.keopsfenks.arcana.skills.effects

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.config.SkillArgumentsReader
import org.bukkit.potion.PotionEffect as BukkitPotionEffect
import org.bukkit.potion.PotionEffectType

class PotionEffect(private val resolve: (String) -> PotionEffectType? = PotionValues::resolve) : SkillEffect("potion") {
    override fun compile(arguments: SkillArguments, compiler: SkillCompiler): CompiledSkillEffect {
        ComponentArguments.keys(arguments, "potion", "duration", "amplifier", "ambient", "particles", "icon", "target")
        val duration = SkillArgumentsReader.ticks(arguments.raw("duration"), "${arguments.path}.duration").toInt()
        val amplifier = arguments.number("amplifier", 0.0)
        require(amplifier in 0.0..255.0 && amplifier % 1.0 == 0.0) { "${arguments.path}.amplifier: expected integer 0..255" }
        val ambient = arguments.bool("ambient", false)
        val particles = arguments.bool("particles", true)
        val icon = arguments.bool("icon", true)
        val target = ComponentArguments.target(arguments)
        val type = resolve(arguments.string("potion")) ?: throw arguments.error("potion", "unknown potion type")
        return CompiledSkillEffect { execution -> execution.onTarget(target) { entity ->
            EffectValues.alive(entity) { entity.addPotionEffect(BukkitPotionEffect(type, duration, amplifier.toInt(), ambient, particles, icon)) }
        } }
    }
}
