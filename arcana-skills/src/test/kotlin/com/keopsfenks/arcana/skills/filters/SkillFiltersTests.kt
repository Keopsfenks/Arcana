package com.keopsfenks.arcana.skills.filters

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.execution.EngineFixture
import io.mockk.mockk
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class SkillFiltersTests {
    @TempDir lateinit var directory: Path

    @Test fun `filter fields are AND and values within a field are OR with missing data rejected`() {
        val filter = SkillFilters.compile(SkillArguments(mapOf(
            "worlds" to listOf("world", "other"), "blocks" to listOf("diamond_ore", "stone"),
            "entities" to listOf("zombie"), "items" to listOf("diamond_sword")
        ), "filters"))
        val values = mapOf("world" to "world", "block" to "DIAMOND_ORE", "entity" to "ZOMBIE", "item" to "DIAMOND_SWORD")
        val context = SkillContext.builder(mockk<Player>()).values(values).build()
        assertTrue(filter.test(context))
        assertFalse(filter.test(SkillContext.builder(mockk<Player>()).values(values - "block").build()))
        assertFalse(filter.test(context.toBuilder().value("world", "WORLD").build()))
        assertFalse(filter.test(context.toBuilder().value("entity", "SKELETON").build()))
    }

    @Test fun `invalid lists material names entity names and unknown fields fail at load time`() {
        for (values in listOf(
            mapOf("blocks" to listOf("NOT_A_MATERIAL")), mapOf("entities" to listOf("NOT_AN_ENTITY")),
            mapOf("worlds" to "world"), mapOf("worlds" to listOf("")), mapOf("unknown" to listOf("x"))
        )) {
            assertThrows(IllegalArgumentException::class.java) { SkillFilters.compile(SkillArguments(values, "filters")) }
        }
    }

    @Test fun `default engine compiler wires native filters into admission`() {
        val f = EngineFixture(directory)
        f.write(options = "filters:\n  blocks: [DIAMOND_ORE]")
        f.load()
        assertEquals(SkillResult.Status.FILTER_FAILED, f.cast().join().status())
        val matching = f.context().toBuilder().value("block", "DIAMOND_ORE").build()
        assertTrue(f.engine.cast("one", matching).toCompletableFuture().join().isSuccess)
    }
}
