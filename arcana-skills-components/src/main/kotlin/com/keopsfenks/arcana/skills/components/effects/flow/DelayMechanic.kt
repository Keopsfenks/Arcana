package com.keopsfenks.arcana.skills.components.effects.flow

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.platform.compile.NodeOptions

class DelayMechanic : SkillMechanic(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("delay", "Wait for a finite duration on the actor owner", "duration")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        require(arguments.has("duration")) { "${arguments.path}.duration: required" }
        val duration = NodeOptions.duration(arguments, compilation, "duration")
        return CompiledSkillMechanic { it.delay(duration.evaluate(it.invocation()).toLong()) }
    }
}
