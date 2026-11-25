package com.keopsfenks.arcana.skills.components.effects.projectiles
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.auras.AuraSupport
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.platform.compile.NodeOptions
import com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionContext
import com.keopsfenks.arcana.skills.platform.managed.ManagedPulse
import com.keopsfenks.arcana.skills.platform.projectiles.ProjectileHitLedger
import java.util.concurrent.CompletableFuture

class BeamMechanic : SkillMechanic(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("beam", "Pulse a finite anchored ray within one managed lineage", *RaySupport.KEYS, *ProjectileArguments.HOOK_KEYS, "duration", "period", "immune-delay")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val ray = RaySupport(arguments, compilation); val duration = AuraSupport.duration(arguments, compilation, "duration", "1s")
        val period = AuraSupport.duration(arguments, compilation, "period", "1t"); val immunity = NodeOptions.duration(arguments, compilation, "immune-delay")
        return CompiledSkillMechanic { context ->
            if (context !is PlatformExecutionContext) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.UNSUPPORTED_CONTEXT, "Beam requires Arcana execution"))
            else {
                val input = context.invocation(); val interval = period.evaluate(input).toLong()
                val ledger = ProjectileHitLedger(ray.maximum.evaluate(input).toInt(), immunity.evaluate(input).toLong()); var tick = -interval
                val pulse = CompiledSkillMechanic { child -> tick += interval; ray.fire(child as PlatformExecutionContext, ledger, tick) }
                ManagedPulse.start(context, duration.evaluate(input).toLong(), interval, ray.hooks.start, listOf(pulse), ray.hooks.end).thenApply { SkillResult.success() }
            }
        }
    }
}
