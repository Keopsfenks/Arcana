package com.keopsfenks.arcana.skills.components.effects.world
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.WorldActions
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.execution.*
import java.util.concurrent.CompletableFuture

class LightningMechanic : SkillMechanic(SkillDescriptor.of("lightning", "Native loaded-location lightning, visual-only by default")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        ComponentArguments.keys(arguments, "target", "effect-only"); val location = WorldActions.location(arguments); val visual = arguments.bool("effect-only", true)
        return CompiledSkillMechanic { context -> val point = location(context.invocation())
            if (point == null) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.NO_TARGET, "Lightning location missing")) else WorldActions.at(context, point, true) { at ->
                SkillWorldMutationScope.during { SkillDamageScope.during { if (visual) at.world!!.strikeLightningEffect(at) else at.world!!.strikeLightning(at) } }; SkillResult.success()
            }
        }
    }
}
