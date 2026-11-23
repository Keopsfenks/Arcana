package com.keopsfenks.arcana.skills.components.targeters

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.targets.*

class ConeTargeter : SkillTargeter(SkillDescriptor.of("cone", "Select a forward cone with an aperture in degrees")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillTargeter {
        TargeterSupport.keys(arguments, "radius", "angle")
        val radius = TargeterSupport.number(arguments, compilation, "radius", 10, 0.0, 128.0)
        val angle = TargeterSupport.number(arguments, compilation, "angle", 90, 0.0, 360.0)
        val policy = TargetSelectionPolicy.compile(arguments, compilation, "nearest")
        return CompiledSkillTargeter { context ->
            val input = context.invocation(); val center = TargetGeometry.center(input); val size = radius.evaluate(input)
            val aperture = angle.evaluate(input); val direction = TargetGeometry.forward(center)
            TargeterSupport.area(context, TargetGeometry.radius(center, size), policy) { TargetGeometry.inCone(it.location(), center, direction, size, aperture) }
        }
    }
}
