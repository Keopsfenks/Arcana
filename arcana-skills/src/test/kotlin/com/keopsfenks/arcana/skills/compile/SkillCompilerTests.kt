package com.keopsfenks.arcana.skills.compile

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.skills.config.SkillArgumentsReader
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SkillCompilerTests {
    @Test fun `numeric expression compiles once and receives the ordered snapshot variables`() {
        val fixture = CompilerFixture()
        val number = fixture.compiler().number("2 + damage", "effects[0].amount")
        assertEquals(6.0, number.evaluate(fixture.context("damage" to 4.0)))
        assertEquals(2.0, number.evaluate(fixture.context()))
        verify(exactly = 1) { fixture.environment.compileOrThrow("2 + damage") }
    }

    @Test fun `invalid expressions boxed numbers and unknown ids have useful errors`() {
        val compiler = CompilerFixture().compiler()
        assertEquals(4.0, compiler.number(4, "amount").evaluate(CompilerFixture().context()))
        for (value in listOf("broken(", Double.POSITIVE_INFINITY, true, listOf(2))) {
            val error = assertThrows(IllegalArgumentException::class.java) { compiler.number(value, "skill.yml.amount") }
            assertTrue(error.message!!.contains("skill.yml.amount"))
        }
        assertThrows(IllegalArgumentException::class.java) { compiler.effects(listOf(mapOf("type" to "missing")), "effects") }
        assertThrows(IllegalArgumentException::class.java) { compiler.condition(SkillArguments(mapOf("type" to "missing"), "conditions[0]")) }
    }

    @Test fun `recursive components compile through the same registered factories`() {
        val fixture = CompilerFixture()
        val condition = fixture.compiler().condition(SkillArguments(mapOf(
            "type" to "all", "conditions" to listOf(
                mapOf("type" to "flag"), mapOf("type" to "not", "conditions" to listOf(mapOf("type" to "flag")))
            )
        ), "conditions[0]"))
        assertFalse(condition.test(fixture.context("allowed" to true)))
    }

    @Test fun `duration parsing accepts native units and rejects invalid bounds`() {
        assertEquals(100_000_000L, SkillArgumentsReader.durationNanos("2t", "cooldown"))
        assertEquals(1_500_000_000L, SkillArgumentsReader.durationNanos("1.5s", "cooldown"))
        assertEquals(60_000_000_000L, SkillArgumentsReader.durationNanos("1m", "cooldown"))
        for (value in listOf("-1s", "NaNs", "25h", "1d", true, Double.NaN)) {
            assertThrows(IllegalArgumentException::class.java) { SkillArgumentsReader.durationNanos(value, "cooldown") }
        }
    }
}
