package com.keopsfenks.arcana.skills.components.targeters

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.targets.*

class ForwardTargeter : SkillTargeter(SkillDescriptor.of("forward", "Select a location ahead using yaw and pitch")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillTargeter {
        TargeterSupport.keys(arguments, "distance")
        val distance = TargeterSupport.number(arguments, compilation, "distance", 1, 0.0, 256.0)
        val policy = TargetSelectionPolicy.compile(arguments, compilation)
        return CompiledSkillTargeter { context ->
            val location = TargetGeometry.center(context.invocation())
            TargeterSupport.locations(context, TargetGeometry.offset(location, TargetGeometry.forward(location), distance.evaluate(context.invocation())), policy)
        }
    }
}
