package com.keopsfenks.arcana.skills.effects

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.config.SkillArgumentsReader
import java.util.concurrent.CompletableFuture

class DelayEffect : SkillEffect("delay") {
    override fun compile(arguments: SkillArguments, compiler: SkillCompiler): CompiledSkillEffect {
        ComponentArguments.keys(arguments, "duration", "effects")
        val ticks = SkillArgumentsReader.ticks(arguments.raw("duration"), "${arguments.path}.duration")
        val children = compiler.effects(arguments.list("effects", emptyList<Any>()), "${arguments.path}.effects")
        return CompiledSkillEffect { execution -> execution.delay(ticks).thenCompose {
            if (it.isSuccess) execution.execute(children) else CompletableFuture.completedFuture(it)
        } }
    }
}
