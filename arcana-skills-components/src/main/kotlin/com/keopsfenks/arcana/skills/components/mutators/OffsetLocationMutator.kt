package com.keopsfenks.arcana.skills.components.mutators

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments

class OffsetLocationMutator : SkillMutator(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("offset_location", "Offset a child location or origin", "field", "x", "y", "z")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMutator {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val field = MutatorSupport.field(arguments)
        val x = MutatorSupport.number(arguments, compilation, "x", 0); val y = MutatorSupport.number(arguments, compilation, "y", 0); val z = MutatorSupport.number(arguments, compilation, "z", 0)
        return CompiledSkillMutator { input -> val location = MutatorSupport.location(input, field)
            MutatorSupport.replace(input, field, SkillLocation(location.world(), location.x() + x.evaluate(input), location.y() + y.evaluate(input), location.z() + z.evaluate(input), location.yaw(), location.pitch()))
        }
    }
}
