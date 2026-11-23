package com.keopsfenks.arcana.skills.components

import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.targeters.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import io.mockk.every

class TargeterTests {
    @Test fun locationFiltersDoNotReceiveStaleVictimSnapshotFields() {
        val f = TargeterFixture()
        f.invocation = f.invocation.derive().data(mapOf("target_health" to 20, "entity" to "ZOMBIE")).build()
        every { f.compiler.predicates(any(), any()) } returns SkillPredicate { input ->
            input.target().isEmpty && input.data().keys.none { it.startsWith("target_") || it == "entity" }
        }
        assertEquals(listOf(f.origin), f.select(OriginTargeter()).locations())
    }
    @Test fun aConditionRejectingTheWallLocationCannotSelectAnEntityBehindIt() {
        val f = TargeterFixture()
        every { f.compiler.predicates(any(), any()) } returns SkillPredicate { it.target().isPresent }
        f.hits = listOf(
            SkillCollision(SkillLocation(f.world, 0.0, 0.0, 2.0), SkillVector(0.0, 0.0, -1.0), java.util.Optional.empty(), .2),
            SkillCollision(SkillLocation(f.world, 0.0, 0.0, 5.0), SkillVector(0.0, 0.0, -1.0), java.util.Optional.of(f.victim.actor()), .5))
        val selected = f.select(RayTargeter())
        assertTrue(selected.entities().isEmpty())
        assertTrue(selected.locations().isEmpty())
    }
    @Test fun roleAndLocationSelectorsKeepSelfVictimTriggerOriginAndLocationDistinct() {
        val f = TargeterFixture()
        val location = SkillLocation(f.world, 9.0, 2.0, 9.0)
        f.invocation = f.invocation.derive().location(location).build()
        assertEquals(listOf(f.self.actor()), f.select(SelfTargeter()).entities())
        assertEquals(listOf(f.victim.actor()), f.select(VictimTargeter()).entities())
        assertEquals(listOf(f.trigger.actor()), f.select(TriggerTargeter()).entities())
        assertEquals(listOf(f.origin), f.select(OriginTargeter()).locations())
        assertEquals(listOf(location), f.select(LocationTargeter()).locations())
        assertEquals(listOf(SkillLocation(f.world, 9.0, 2.0, 11.0)), f.select(ForwardTargeter(), mapOf("distance" to 2)).locations())
        f.snapshots.remove(f.victim)
        assertTrue(f.select(VictimTargeter()).entities().isEmpty())
        f.invocation = SkillInvocation.builder(f.self.actor()).build()
        assertThrows(Exception::class.java) { f.select(ForwardTargeter()) }
    }
    @Test fun selectionHasStableOrderingFiltersDeduplicationSeededRandomnessAndAHardLimit() {
        val f = TargeterFixture()
        repeat(70) { f.entity(it / 10.0, 0.0) }
        assertEquals(64, f.select(EntitiesInRadiusTargeter(), mapOf("radius" to 10)).entities().size)
        val options = mapOf<String, Any>("radius" to 10, "limit" to 8, "sort" to "random", "exclude-self" to true)
        assertEquals(f.select(EntitiesInRadiusTargeter(), options, 47), f.select(EntitiesInRadiusTargeter(), options, 47))
        assertNotEquals(f.select(EntitiesInRadiusTargeter(), options, 47), f.select(EntitiesInRadiusTargeter(), options, 48))
        f.snapshots.add(f.victim)
        val nearest = f.select(EntitiesInRadiusTargeter(), mapOf("radius" to 10, "sort" to "nearest", "limit" to 256))
        assertEquals(nearest.entities().size, nearest.entities().distinct().size)
        assertThrows(IllegalArgumentException::class.java) { f.select(SelfTargeter(), mapOf("limit" to 257)) }
        assertThrows(IllegalArgumentException::class.java) { f.select(SelfTargeter(), mapOf("limit" to 1.5)) }
        assertEquals(listOf(f.self.actor()), f.select(PlayersInRadiusTargeter(), mapOf("radius" to 10)).entities())
        assertFalse(f.select(EntitiesInRadiusTargeter(), mapOf("radius" to 10, "exclude-self" to true)).entities().contains(f.self.actor()))
    }
    @Test fun rayChoosesTheNearestContactAndWallsOccludeLaterEntities() {
        val f = TargeterFixture()
        f.hits = listOf(
            SkillCollision(SkillLocation(f.world, 0.0, 0.0, 5.0), SkillVector(0.0, 0.0, -1.0), java.util.Optional.of(f.victim.actor()), .5),
            SkillCollision(SkillLocation(f.world, 0.0, 0.0, 2.0), SkillVector(0.0, 0.0, -1.0), java.util.Optional.empty(), .2))
        val result = f.select(RayTargeter(), mapOf("length" to 10))
        assertTrue(result.entities().isEmpty())
        assertEquals(2.0, result.locations().single().z())
        f.hits = listOf(f.hits[0])
        assertEquals(listOf(f.victim.actor()), f.select(RayTargeter(), mapOf("length" to 10)).entities())
    }
}
