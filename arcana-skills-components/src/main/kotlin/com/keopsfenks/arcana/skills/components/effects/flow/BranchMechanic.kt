package com.keopsfenks.arcana.skills.components.effects.flow

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionContext

class BranchMechanic : SkillMechanic(SkillDescriptor.of("branch", "Execute the matching side of a typed predicate")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        ComponentArguments.keys(arguments, "condition", "then", "else")
        val condition = compilation.predicate(arguments.section("condition"))
        val yes = FlowSupport.effects(arguments, compilation, "then", true)
        val no = FlowSupport.effects(arguments, compilation, "else", true)
        return CompiledSkillMechanic { execution ->
            if (execution is PlatformExecutionContext) FlowSupport.evaluate(execution, condition) { passed, child -> child.execute(if (passed) yes else no) }
            else execution.execute(if (condition.test(execution.invocation())) yes else no)
        }
    }
}
