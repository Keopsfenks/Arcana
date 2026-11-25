package com.keopsfenks.arcana.skills.components.targeters

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.targets.*
import kotlin.math.*

class LineTargeter : SkillTargeter(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("line", "Select a finite forward segment with an inclusive half-width", *com.keopsfenks.arcana.skills.platform.targets.TargetSelectionPolicy.OPTIONS, "length", "width")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillTargeter {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val length = TargeterSupport.number(arguments, compilation, "length", 10, 0.0, 256.0)
        val width = TargeterSupport.number(arguments, compilation, "width", 1, 0.0, 16.0)
        val policy = TargetSelectionPolicy.compile(arguments, compilation, "nearest")
        return CompiledSkillTargeter { context ->
            val input = context.invocation(); val start = TargetGeometry.center(input); val end = TargetGeometry.offset(start, TargetGeometry.forward(start), length.evaluate(input))
            val size = width.evaluate(input)
            val bounds = SkillBounds(start.world(), min(start.x(), end.x()) - size, min(start.y(), end.y()) - size, min(start.z(), end.z()) - size,
                max(start.x(), end.x()) + size, max(start.y(), end.y()) + size, max(start.z(), end.z()) + size)
            TargeterSupport.area(context, bounds, policy) { TargetGeometry.inLine(it.location(), start, end, size) }
        }
    }
}
