package com.keopsfenks.arcana.skills.components.targeters

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.targets.*

class BoxTargeter : SkillTargeter(SkillDescriptor.of("box", "Select entities whose positions are inside a centered box")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillTargeter {
        TargeterSupport.keys(arguments, "width", "height", "depth")
        val width = TargeterSupport.number(arguments, compilation, "width", 10, 0.0, 256.0)
        val height = TargeterSupport.number(arguments, compilation, "height", 10, 0.0, 256.0)
        val depth = TargeterSupport.number(arguments, compilation, "depth", 10, 0.0, 256.0)
        val policy = TargetSelectionPolicy.compile(arguments, compilation, "nearest")
        return CompiledSkillTargeter { context ->
            val input = context.invocation(); val center = TargetGeometry.center(input)
            val x = width.evaluate(input) / 2; val y = height.evaluate(input) / 2; val z = depth.evaluate(input) / 2
            val bounds = SkillBounds(center.world(), center.x() - x, center.y() - y, center.z() - z, center.x() + x, center.y() + y, center.z() + z)
            TargeterSupport.area(context, bounds, policy) { snapshot -> snapshot.location().let { point ->
                point.world() == bounds.world() && point.x() in bounds.minX()..bounds.maxX() && point.y() in bounds.minY()..bounds.maxY() && point.z() in bounds.minZ()..bounds.maxZ()
            } }
        }
    }
}
