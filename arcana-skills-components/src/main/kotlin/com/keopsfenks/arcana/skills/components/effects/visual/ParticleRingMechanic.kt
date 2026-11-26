package com.keopsfenks.arcana.skills.components.effects.visual
import com.keopsfenks.arcana.core.particle.*
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.NativeActions
import com.keopsfenks.arcana.skills.config.ComponentArguments
import java.util.concurrent.CompletableFuture
import kotlin.math.*

class ParticleRingMechanic(private val resolve: (String) -> SpawnableParticle = Particles::lookup) : SkillMechanic(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("particle_ring", "Spawn a finite horizontal particle ring", "target", "particle", "amount", "points", "radius", "rotation")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val count = NativeActions.number(arguments, compilation, "points", 16, 3.0, 256.0, true); ParticleSupport.validateTotal(arguments, 16)
        val radius = NativeActions.number(arguments, compilation, "radius", 2, 0.0, 64.0); val rotation = NativeActions.number(arguments, compilation, "rotation", 0, -360.0, 360.0)
        val particle = ParticleSupport(arguments, compilation, resolve)
        return CompiledSkillMechanic { context -> val input = context.invocation(); val start = particle.center(input)
            if (start == null) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.NO_TARGET, "Ring origin missing")) else {
                val points = count.evaluate(input).toInt(); val size = radius.evaluate(input); val angle = Math.toRadians(rotation.evaluate(input))
                particle.spawn(context, (0 until points).map { val theta = angle+2*PI*it/points; SkillLocation(start.world(), start.x()+cos(theta)*size, start.y(), start.z()+sin(theta)*size) })
            }
        }
    }
}
