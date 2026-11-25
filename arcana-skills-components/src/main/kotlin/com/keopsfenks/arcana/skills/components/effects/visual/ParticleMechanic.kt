package com.keopsfenks.arcana.skills.components.effects.visual
import com.keopsfenks.arcana.core.particle.*
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import java.util.concurrent.CompletableFuture

class ParticleMechanic(private val resolve: (String) -> SpawnableParticle = Particles::lookup) : SkillMechanic(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("particle", "Spawn a resolved Arcana particle on the owning location", "target", "particle", "amount")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor()); val particle = ParticleSupport(arguments, compilation, resolve)
        return CompiledSkillMechanic { context -> val at = particle.center(context.invocation()); if (at == null) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.NO_TARGET, "Particle location missing")) else particle.spawn(context, listOf(at)) }
    }
}
