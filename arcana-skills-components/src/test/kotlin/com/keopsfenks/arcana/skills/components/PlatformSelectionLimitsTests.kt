package com.keopsfenks.arcana.skills.components
import com.keopsfenks.arcana.skills.components.targeters.EntitiesInRadiusTargeter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
class PlatformSelectionLimitsTests {
    @Test fun deterministicSelectionLoads64DefaultAnd256MaximumRecipients() {
        val f = TargeterFixture(); repeat(300) { f.entity(it / 100.0, 0.0) }
        assertEquals(64, f.select(EntitiesInRadiusTargeter(), mapOf("radius" to 10)).entities().size)
        val options = mapOf("radius" to 10, "limit" to 256)
        val first = f.select(EntitiesInRadiusTargeter(), options); assertEquals(256, first.entities().size)
        assertEquals(first, f.select(EntitiesInRadiusTargeter(), options)); assertEquals(256, first.entities().distinct().size)
        println("load: targetDefault=64 targetMaximum=256 candidates=${f.snapshots.size} deterministic=true")
    }
}
