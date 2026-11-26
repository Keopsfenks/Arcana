package com.keopsfenks.arcana.skills.effects

import com.keopsfenks.arcana.core.particle.Particles
import com.keopsfenks.arcana.core.particle.SpawnableParticle
import com.keopsfenks.arcana.core.particle.impl.EmptyParticle
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import org.bukkit.Particle
import java.util.Locale

class ParticleEffect(private val resolve: (String) -> SpawnableParticle = Particles::lookup) : SkillEffect("particle") {
    override fun compile(arguments: SkillArguments, compiler: SkillCompiler): CompiledSkillEffect {
        ComponentArguments.keys(arguments, "particle", "amount", "target")
        val key = arguments.string("particle")
        val canonical = if (':' !in key) {
            val particle = try { Particle.valueOf(key.uppercase(Locale.ROOT)) }
            catch (_: IllegalArgumentException) { throw arguments.error("particle", "unknown particle $key") }
            require(particle.dataType in setOf(Void::class.java, java.lang.Float::class.java, java.lang.Integer::class.java)) {
                "${arguments.path}.particle: $key requires data; use an Arcana lookup such as dust:ff00ff"
            }
            particle.name
        } else key.lowercase(Locale.ROOT)
        val particle = resolve(canonical)
        require(particle !is EmptyParticle) { "${arguments.path}.particle: unknown particle lookup $key" }
        val amount = ComponentArguments.number(arguments, "amount", compiler, 1.0, 10_000.0, 10, integer = true)
        val target = ComponentArguments.target(arguments)
        return CompiledSkillEffect { execution -> execution.atTarget(target) { location ->
            particle.spawn(location, amount.evaluate(execution.context).toInt())
            SkillResult.success()
        } }
    }
}
