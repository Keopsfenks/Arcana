package com.keopsfenks.arcana.skills.components.effects.flow

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.config.SkillArgumentsReader
import com.keopsfenks.arcana.skills.platform.compile.NodeOptions
import com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionContext
import java.util.concurrent.CompletableFuture

class WaitUntilMechanic : SkillMechanic(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("wait_until", "Poll fresh snapshots until a condition succeeds or a finite timeout expires", "condition", "timeout", "poll", "effects")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        require(arguments.has("timeout")) { "${arguments.path}.timeout: required" }
        val timeout = NodeOptions.duration(arguments, compilation, "timeout")
        val adjusted = if (arguments.has("poll")) arguments else SkillArguments(arguments.values + ("poll" to "1t"), arguments.path)
        val poll = NodeOptions.duration(adjusted, compilation, "poll")
        if (arguments.raw("timeout") is Number || arguments.raw("timeout") is String && arguments.raw("timeout").toString().matches(Regex("[0-9.]+(?:ms|s|m|t)")))
            require(SkillArgumentsReader.ticks(arguments.raw("timeout"), "${arguments.path}.timeout") >= 1) { "${arguments.path}.timeout: must be at least one tick" }
        if (adjusted.raw("poll") is Number || adjusted.raw("poll") is String && adjusted.raw("poll").toString().matches(Regex("[0-9.]+(?:ms|s|m|t)")))
            require(SkillArgumentsReader.ticks(adjusted.raw("poll"), "${arguments.path}.poll") >= 1) { "${arguments.path}.poll: must be at least one tick" }
        val condition = compilation.predicate(arguments.section("condition"))
        val body = FlowSupport.effects(arguments, compilation, optional = true)
        return CompiledSkillMechanic { execution ->
            if (execution !is PlatformExecutionContext) FlowSupport.unsupported() else {
                val maximum = timeout.evaluate(execution.invocation()).toLong().also { require(it >= 1) { "${arguments.path}.timeout: must be at least one tick" } }
                val interval = poll.evaluate(execution.invocation()).toLong().also { require(it >= 1) { "${arguments.path}.poll: must be at least one tick" } }
                fun check(elapsed: Long): CompletableFuture<SkillResult> {
                    execution.state.step()
                    return FlowSupport.evaluate(execution, condition) { passed, child ->
                        when {
                            passed -> child.execute(body).toCompletableFuture()
                            elapsed >= maximum -> CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.CONDITION_FAILED, "Wait-until timeout expired"))
                            else -> {
                                child.state.release(child.frame)
                                val ticks = minOf(interval, maximum - elapsed)
                                execution.delay(ticks).thenCompose { check(elapsed + ticks) }.toCompletableFuture()
                            }
                        }
                    }
                }
                check(0)
            }
        }
    }
}
