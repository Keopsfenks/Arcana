package com.keopsfenks.arcana.skills.components.effects.world
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.execution.*
import com.keopsfenks.arcana.skills.platform.targets.TargetGeometry
import java.util.concurrent.CompletableFuture

class ExplosionMechanic : SkillMechanic(SkillDescriptor.of("explosion", "Native bounded loaded-area explosion; fire and block damage default false")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        ComponentArguments.keys(arguments, "target", "power", "fire", "break-blocks"); val location = WorldActions.location(arguments)
        val power = NativeActions.number(arguments, compilation, "power", 1, 0.0, 8.0); val fire = arguments.bool("fire", false); val blocks = arguments.bool("break-blocks", false)
        return CompiledSkillMechanic { context -> val input = context.invocation(); val point = location(input); val strength = power.evaluate(input)
            if (point == null) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.NO_TARGET, "Explosion location missing")) else {
                context.services().require(SkillTargetQuery::class.java).loaded(TargetGeometry.radius(point, strength*2+1)).thenCompose { loaded ->
                    if (!loaded) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.NO_TARGET, "Explosion bounds contain unloaded chunks")) else WorldActions.at(context, point, true) { at ->
                        val accepted = SkillWorldMutationScope.during { SkillDamageScope.during { at.world!!.createExplosion(at, strength.toFloat(), fire, blocks) } }
                        if (accepted) SkillResult.success() else SkillResult.of(SkillResult.Status.NO_TARGET, "Native explosion cancelled")
                    }
                }
            }
        }
    }
}
