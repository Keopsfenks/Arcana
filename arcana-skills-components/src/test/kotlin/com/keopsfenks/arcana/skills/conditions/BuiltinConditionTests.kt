package com.keopsfenks.arcana.skills.conditions

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.BuiltinComponents
import com.keopsfenks.arcana.skills.compile.CompilerFixture
import com.keopsfenks.arcana.skills.compile.SkillCompilerImpl
import com.keopsfenks.arcana.skills.registry.ComponentRegistry
import io.mockk.mockk
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BuiltinConditionTests {
    private fun compiler(): SkillCompilerImpl {
        val f = CompilerFixture()
        val registry = ComponentRegistry<SkillCondition>()
        BuiltinComponents.conditionTypes().forEach { registry.register(f.owner, it) }
        return SkillCompilerImpl(f.environment, f.effects, registry, f.triggers)
    }
    private fun context(vararg values: Pair<String, Any>) = SkillContext.builder(mockk<Player>()).values(mapOf(*values)).build()

    @Test fun `health food world and movement use the copied invocation values`() {
        val compiler = compiler()
        val cases = listOf(
            mapOf("type" to "health_below", "ratio" to 0.75) to true,
            mapOf("type" to "health_above", "ratio" to 0.75) to false,
            mapOf("type" to "food_below", "amount" to 10) to true,
            mapOf("type" to "food_above", "amount" to 10) to false,
            mapOf("type" to "world", "name" to "world") to true,
            mapOf("type" to "sneaking") to true,
            mapOf("type" to "sprinting") to false,
            mapOf("type" to "has_target") to false,
            mapOf("type" to "expression", "expression" to 0) to false
        )
        val ctx = context("caster_health" to 10.0, "caster_max_health" to 20.0, "caster_food" to 5, "caster_sneaking" to true, "world" to "world")
        for ((definition, expected) in cases) assertEquals(expected, compiler.condition(SkillArguments(definition, "test")).test(ctx), definition.toString())
    }

    @Test fun `all any and not compose without empty ambiguous composites`() {
        val compiler = compiler()
        val yes = mapOf("type" to "sneaking")
        val no = mapOf("type" to "sprinting")
        val context = context("caster_sneaking" to true)
        assertFalse(compiler.condition(SkillArguments(mapOf("type" to "all", "conditions" to listOf(yes, no)), "test")).test(context))
        assertTrue(compiler.condition(SkillArguments(mapOf("type" to "any", "conditions" to listOf(yes, no)), "test")).test(context))
        assertTrue(compiler.condition(SkillArguments(mapOf("type" to "not", "condition" to no), "test")).test(context))
        for (type in listOf("all", "any")) {
            assertThrows(IllegalArgumentException::class.java) {
                compiler.condition(SkillArguments(mapOf("type" to type, "conditions" to emptyList<Any>()), "test"))
            }
        }
    }
}
