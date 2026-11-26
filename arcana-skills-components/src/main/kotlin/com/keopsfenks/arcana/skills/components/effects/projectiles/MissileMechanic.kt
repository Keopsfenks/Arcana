package com.keopsfenks.arcana.skills.components.effects.projectiles
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.platform.projectiles.ProjectileManager
import java.util.concurrent.CompletableFuture

class MissileMechanic : SkillMechanic(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("missile", "Launch a target-required homing projectile", *ProjectileArguments.MOTION_KEYS, *ProjectileArguments.HOOK_KEYS)) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val options = ProjectileArguments.motion(arguments, compilation, 1.0); val hooks = ProjectileArguments.Hooks(arguments, compilation).projectile()
        return CompiledSkillMechanic { context -> val input = context.invocation()
            if (input.target().isEmpty) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.NO_TARGET, "Missile requires a target"))
            else context.services().require(ProjectileManager::class.java).launch(input, options(input), hooks).thenApply { SkillResult.success() }
        }
    }
}
