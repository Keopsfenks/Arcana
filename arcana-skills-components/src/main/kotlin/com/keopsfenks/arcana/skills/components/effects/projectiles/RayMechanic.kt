package com.keopsfenks.arcana.skills.components.effects.projectiles
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionContext
import com.keopsfenks.arcana.skills.platform.projectiles.ProjectileHitLedger
import java.util.concurrent.CompletableFuture

class RayMechanic : SkillMechanic(SkillDescriptor.of("ray", "Run one bounded swept ray with contact hooks")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        ComponentArguments.keys(arguments, *RaySupport.KEYS, *ProjectileArguments.HOOK_KEYS)
        val ray = RaySupport(arguments, compilation)
        return CompiledSkillMechanic { context ->
            if (context !is PlatformExecutionContext) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.UNSUPPORTED_CONTEXT, "Ray requires Arcana execution"))
            else {
                val ledger = ProjectileHitLedger(ray.maximum.evaluate(context.invocation()).toInt(), 0)
                context.execute(ray.hooks.start + CompiledSkillMechanic { child -> ray.fire(child as PlatformExecutionContext, ledger, 0) } + ray.hooks.end)
            }
        }
    }
}
