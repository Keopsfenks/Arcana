package com.keopsfenks.arcana.skills.compile

import com.keopsfenks.arcana.core.math.CompiledExpression
import com.keopsfenks.arcana.core.math.ExpressionEnvironment
import com.keopsfenks.arcana.core.math.RandomSource
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.registry.ComponentRegistry
import io.mockk.every
import io.mockk.mockk
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.concurrent.CompletableFuture

class CompilerFixture {
    val owner = mockk<Plugin>()
    val effects = ComponentRegistry<SkillEffect>()
    val conditions = ComponentRegistry<SkillCondition>()
    val triggers = ComponentRegistry<SkillTrigger>()
    val environment = mockk<ExpressionEnvironment>()

    init {
        every { environment.compileOrThrow(any()) } throws IllegalArgumentException("bad expression")
        val expression = object : CompiledExpression {
            override fun evaluate(vararg values: Double) = 2.0 + values[0]
            override fun evaluate(random: RandomSource, vararg values: Double) = evaluate(*values)
            override fun getVariableCount() = 5
            override fun isDeterministic() = true
        }
        every { environment.compileOrThrow("2 + damage") } returns expression
        effects.register(owner, object : SkillEffect("mark") {
            override fun compile(arguments: SkillArguments, compiler: SkillCompiler) =
                CompiledSkillEffect { CompletableFuture.completedFuture(SkillResult.success()) }
        })
        conditions.register(owner, object : SkillCondition("flag") {
            override fun compile(arguments: SkillArguments, compiler: SkillCompiler) =
                CompiledSkillCondition { it.getFlag("allowed") }
        })
        for (id in listOf("all", "any", "not")) {
            conditions.register(owner, object : SkillCondition(id) {
                override fun compile(arguments: SkillArguments, compiler: SkillCompiler): CompiledSkillCondition {
                    val children = arguments.list("conditions").mapIndexed { index, value ->
                        compiler.condition(arguments.child("conditions[$index]", value))
                    }
                    return CompiledSkillCondition { context -> when (id) {
                        "all" -> children.all { it.test(context) }
                        "any" -> children.any { it.test(context) }
                        else -> !children.single().test(context)
                    } }
                }
            })
        }
        triggers.register(owner, object : SkillTrigger("attack") {
            override fun listen(fire: java.util.function.Consumer<SkillContext>) = AutoCloseable {}
        })
    }

    fun compiler() = SkillCompilerImpl(environment, effects, conditions, triggers)
    fun context(vararg values: Pair<String, Any>) = SkillContext.builder(mockk<Player>()).values(mapOf(*values)).build()
}
