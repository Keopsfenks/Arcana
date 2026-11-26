package com.keopsfenks.arcana.skills.components.effects.visual
import com.keopsfenks.arcana.core.particle.*
import com.keopsfenks.arcana.core.particle.impl.EmptyParticle
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.*
import org.bukkit.Particle
import java.util.Locale
import java.util.concurrent.CompletableFuture

internal class ParticleSupport(args: SkillArguments, compiler: SkillCompilation, resolve: (String) -> SpawnableParticle) {
    val center = WorldActions.location(args)
    private val particle: SpawnableParticle
    private val amount = NativeActions.number(args, compiler, "amount", 1, 1.0, 10_000.0, true)
    init {
        val key = args.string("particle")
        val canonical = if (':' in key) key.lowercase(Locale.ROOT) else {
            val vanilla = try { Particle.valueOf(key.uppercase(Locale.ROOT)) } catch (_: IllegalArgumentException) { throw args.error("particle", "unknown particle") }
            require(vanilla.dataType in setOf(Void::class.java, java.lang.Float::class.java, java.lang.Integer::class.java)) { "${args.path}.particle: use a configured Arcana lookup for particle data" }; vanilla.name
        }
        particle = resolve(canonical); require(particle !is EmptyParticle) { "${args.path}.particle: unknown particle lookup" }
    }
    fun spawn(context: SkillExecutionContext, positions: List<SkillLocation>): CompletableFuture<SkillResult> {
        val count = amount.evaluate(context.invocation()).toInt()
        require(positions.size <= 256 && positions.size.toLong()*count <= 10_000) { "Particle geometry exceeds 256 points or 10000 particles" }
        var result = CompletableFuture.completedFuture(SkillResult.success())
        for (position in positions) result = result.thenCompose { prior ->
            if (!prior.isSuccess) CompletableFuture.completedFuture(prior) else WorldActions.at(context, position) { at -> particle.spawn(at, count); SkillResult.success() }.toCompletableFuture()
        }
        return result
    }
    companion object {
        fun validateTotal(args: SkillArguments, defaultPoints: Int) {
            val points = args.raw("points", defaultPoints); val amount = args.raw("amount", 1)
            if (points is Number && amount is Number) require(points.toDouble()*amount.toDouble() <= 10_000) { "${args.path}.amount: geometry exceeds 10000 particles" }
        }
    }
}
