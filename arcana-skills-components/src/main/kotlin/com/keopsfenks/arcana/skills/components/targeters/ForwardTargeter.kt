package com.keopsfenks.arcana.skills.components.targeters

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.targets.*

class ForwardTargeter : SkillTargeter(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("forward", "Select a location ahead using yaw and pitch", *com.keopsfenks.arcana.skills.platform.targets.TargetSelectionPolicy.OPTIONS, "distance")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillTargeter {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val distance = TargeterSupport.number(arguments, compilation, "distance", 1, 0.0, 256.0)
        val policy = TargetSelectionPolicy.compile(arguments, compilation)
        return CompiledSkillTargeter { context ->
            val location = TargetGeometry.center(context.invocation())
            TargeterSupport.locations(context, TargetGeometry.offset(location, TargetGeometry.forward(location), distance.evaluate(context.invocation())), policy)
        }
    }
}
