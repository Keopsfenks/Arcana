package com.keopsfenks.arcana.skills.components.targeters

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.targets.*

class PlayersInRadiusTargeter : SkillTargeter(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("players_in_radius", "Select only players in an inclusive radius", *com.keopsfenks.arcana.skills.platform.targets.TargetSelectionPolicy.OPTIONS, "radius")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillTargeter {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val radius = TargeterSupport.number(arguments, compilation, "radius", 5, 0.0, 128.0)
        val policy = TargetSelectionPolicy.compile(arguments, compilation, "nearest", playersOnly = true)
        return CompiledSkillTargeter { context ->
            val center = TargetGeometry.center(context.invocation()); val size = radius.evaluate(context.invocation())
            TargeterSupport.area(context, TargetGeometry.radius(center, size), policy) { TargetGeometry.distanceSquared(center, it.location()) <= size * size + 1e-9 }
        }
    }
}
