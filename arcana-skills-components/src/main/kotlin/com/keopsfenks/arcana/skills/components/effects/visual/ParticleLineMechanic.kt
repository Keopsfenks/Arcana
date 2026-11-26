package com.keopsfenks.arcana.skills.components.effects.visual
import com.keopsfenks.arcana.core.particle.*
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.NativeActions
import com.keopsfenks.arcana.skills.components.effects.projectiles.ProjectileArguments
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.platform.targets.TargetGeometry
import java.util.concurrent.CompletableFuture

class ParticleLineMechanic(private val resolve: (String) -> SpawnableParticle = Particles::lookup) : SkillMechanic(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("particle_line", "Spawn bounded immutable line geometry with one resolved particle", "target", "particle", "amount", "points", "length", "direction")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val count = NativeActions.number(arguments, compilation, "points", 16, 2.0, 256.0, true); ParticleSupport.validateTotal(arguments, 16)
        val length = NativeActions.number(arguments, compilation, "length", 4, 0.0, 256.0); val direction = ProjectileArguments.direction(arguments, compilation)
        val particle = ParticleSupport(arguments, compilation, resolve)
        return CompiledSkillMechanic { context -> val input = context.invocation(); val start = particle.center(input)
            if (start == null) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.NO_TARGET, "Line origin missing")) else {
                val points = count.evaluate(input).toInt(); val vector = direction(input); val extent = length.evaluate(input)
                particle.spawn(context, (0 until points).map { TargetGeometry.offset(start, vector, extent*it/(points-1)) })
            }
        }
    }
}
