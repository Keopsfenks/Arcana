package com.keopsfenks.arcana.skills.effects

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.config.SkillArgumentsReader
import java.util.concurrent.CompletableFuture

class RepeatEffect : SkillEffect("repeat") {
    override fun compile(arguments: SkillArguments, compiler: SkillCompiler): CompiledSkillEffect {
        ComponentArguments.keys(arguments, "times", "interval", "effects")
        val times = arguments.number("times", 1.0)
        require(times in 1.0..100.0 && times % 1.0 == 0.0) { "${arguments.path}.times: expected integer 1..100" }
        val interval = SkillArgumentsReader.ticks(arguments.raw("interval", "0t"), "${arguments.path}.interval")
        val children = compiler.effects(arguments.list("effects"), "${arguments.path}.effects")
        return CompiledSkillEffect { execution ->
            var result = CompletableFuture.completedFuture(SkillResult.success())
            repeat(times.toInt()) { index ->
                if (index > 0 && interval > 0) result = result.thenCompose {
                    if (it.isSuccess) execution.delay(interval).toCompletableFuture() else CompletableFuture.completedFuture(it)
                }
                result = result.thenCompose {
                    if (it.isSuccess) execution.execute(children).toCompletableFuture() else CompletableFuture.completedFuture(it)
                }
            }
            result
        }
    }
}
