package com.keopsfenks.arcana.skills.components.mutators

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import kotlin.math.*

class RotateLocationMutator : SkillMutator(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("rotate_location", "Rotate a child point horizontally around its origin, in degrees", "field", "angle")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMutator {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val field = MutatorSupport.field(arguments); val angle = MutatorSupport.number(arguments, compilation, "angle", 0)
        return CompiledSkillMutator { input ->
            val location = MutatorSupport.location(input, field); val pivot = input.origin().orElse(location)
            require(location.world() == pivot.world()) { "Rotation origin and location must be in the same world" }
            val degrees = angle.evaluate(input) % 360; val radians = Math.toRadians(degrees)
            val x = location.x() - pivot.x(); val z = location.z() - pivot.z()
            MutatorSupport.replace(input, field, SkillLocation(location.world(), pivot.x() + x * cos(radians) - z * sin(radians), location.y(),
                pivot.z() + x * sin(radians) + z * cos(radians), ((location.yaw() + degrees) % 360).toFloat(), location.pitch()))
        }
    }
}
