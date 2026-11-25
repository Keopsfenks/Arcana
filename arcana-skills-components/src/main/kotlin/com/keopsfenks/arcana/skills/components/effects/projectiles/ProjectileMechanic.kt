package com.keopsfenks.arcana.skills.components.effects.projectiles
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.platform.projectiles.ProjectileManager

class ProjectileMechanic : SkillMechanic(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("projectile", "Launch finite swept projectile motion", *ProjectileArguments.MOTION_KEYS, *ProjectileArguments.HOOK_KEYS)) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val options = ProjectileArguments.motion(arguments, compilation, 0.0); val hooks = ProjectileArguments.Hooks(arguments, compilation).projectile()
        return CompiledSkillMechanic { context -> context.services().require(ProjectileManager::class.java).launch(context.invocation(), options(context.invocation()), hooks).thenApply { SkillResult.success() } }
    }
}
