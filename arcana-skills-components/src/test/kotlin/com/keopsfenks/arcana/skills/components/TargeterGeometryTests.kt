package com.keopsfenks.arcana.skills.components

import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.targeters.*
import com.keopsfenks.arcana.skills.platform.targets.TargetGeometry
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TargeterGeometryTests {
    @Test fun radiusIncludesTheExactBoundaryAndBoxUsesEachIndependentDimension() {
        val f = TargeterFixture()
        val boundary = f.entity(0.0, 3.0)
        val outside = f.entity(0.0, 3.001)
        val selection = f.select(EntitiesInRadiusTargeter(), mapOf("radius" to 3))
        assertTrue(selection.entities().contains(boundary.actor()))
        assertFalse(selection.entities().contains(outside.actor()))
        val box = f.select(BoxTargeter(), mapOf("width" to 2, "height" to 2, "depth" to 6))
        assertTrue(box.entities().contains(boundary.actor()))
        assertFalse(box.entities().contains(f.victim.actor()))
    }
    @Test fun coneUsesDegreesAndTheLineHasFiniteEndpointsAndWidth() {
        val f = TargeterFixture()
        val ahead = f.entity(0.0, 4.0)
        val edge = f.entity(2.0, 2.0)
        val behind = f.entity(0.0, -1.0)
        val beyond = f.entity(0.0, 6.0)
        val cone = f.select(ConeTargeter(), mapOf("radius" to 5, "angle" to 90))
        assertTrue(cone.entities().containsAll(listOf(ahead.actor(), edge.actor())))
        assertFalse(cone.entities().contains(behind.actor()))
        val line = f.select(LineTargeter(), mapOf("length" to 5, "width" to .5))
        assertTrue(line.entities().contains(ahead.actor()))
        assertFalse(line.entities().contains(edge.actor()))
        assertFalse(line.entities().contains(beyond.actor()))
        assertThrows(IllegalArgumentException::class.java) { TargetGeometry.inCone(ahead.location(), f.origin, SkillVector(0.0, 0.0, 0.0), 5.0, 90.0) }
        val east = TargetGeometry.forward(SkillLocation(f.world, 0.0, 0.0, 0.0, -90f, 0f))
        assertEquals(1.0, east.x(), 1e-9)
        assertEquals(0.0, east.z(), 1e-9)
    }
}
