package com.keopsfenks.arcana.skills.components.mutators

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import kotlin.math.floor

class CenterLocationMutator : SkillMutator(SkillDescriptor.of("center_location", "Center X/Z within the block and align Y to its base")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMutator {
        ComponentArguments.keys(arguments, "field")
        val field = MutatorSupport.field(arguments)
        return CompiledSkillMutator { input -> val point = MutatorSupport.location(input, field)
            MutatorSupport.replace(input, field, SkillLocation(point.world(), floor(point.x()) + .5, floor(point.y()), floor(point.z()) + .5, point.yaw(), point.pitch()))
        }
    }
}
