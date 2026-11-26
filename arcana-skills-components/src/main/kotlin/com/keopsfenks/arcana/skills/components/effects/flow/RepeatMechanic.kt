package com.keopsfenks.arcana.skills.components.effects.flow

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.platform.compile.NodeOptions
import java.util.concurrent.CompletableFuture

class RepeatMechanic : SkillMechanic(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("repeat", "Repeat child effects up to 100 times with an optional period", "times", "period", "effects")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val times = NodeOptions.number(arguments, compilation, "times", 1, 1.0, 100.0, true)
        val period = NodeOptions.duration(arguments, compilation, "period")
        val effects = FlowSupport.effects(arguments, compilation)
        return CompiledSkillMechanic { execution ->
            var result = FlowSupport.success()
            repeat(times.evaluate(execution.invocation()).toInt()) { index -> result = result.thenCompose { previous ->
                if (!previous.isSuccess) CompletableFuture.completedFuture(previous) else
                    (if (index == 0) FlowSupport.success() else execution.delay(period.evaluate(execution.invocation()).toLong()).toCompletableFuture())
                        .thenCompose { wait -> if (wait.isSuccess) execution.execute(effects).toCompletableFuture() else CompletableFuture.completedFuture(wait) }
            } }
            result
        }
    }
}
