package com.keopsfenks.arcana.skills.components.mutators

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import kotlin.math.floor

class CenterLocationMutator : SkillMutator(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("center_location", "Center X/Z within the block and align Y to its base", "field")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMutator {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val field = MutatorSupport.field(arguments)
        return CompiledSkillMutator { input -> val point = MutatorSupport.location(input, field)
            MutatorSupport.replace(input, field, SkillLocation(point.world(), floor(point.x()) + .5, floor(point.y()), floor(point.z()) + .5, point.yaw(), point.pitch()))
        }
    }
}
