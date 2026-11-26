package com.keopsfenks.arcana.core.skills.platform

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

class PlatformValueTests {
    @Test fun keysAndGeometryValidateBeforeExecution() {
        assertEquals(SkillKey("arcana", "dash"), SkillKey.parse("dash", "arcana"))
        assertEquals(SkillKey("example", "dash"), SkillKey.parse("example:dash", "arcana"))
        for (bad in listOf("../dash", "A:dash", "a:b:c", ":dash", "arcana:")) {
            assertThrows(IllegalArgumentException::class.java) { SkillKey.parse(bad, "arcana") }
        }
        assertThrows(IllegalArgumentException::class.java) { SkillVector(Double.NaN, 0.0, 0.0) }
        assertThrows(IllegalArgumentException::class.java) { SkillLocation(UUID.randomUUID(), Double.POSITIVE_INFINITY, 0.0, 0.0) }
    }

    @Test fun invocationValuesCopyMutableInputs() {
        val player = mockk<Player>()
        val id = UUID.randomUUID()
        every { player.uniqueId } returns id
        val number = AtomicInteger(4)
        val list = mutableListOf<Any>("before")
        val actor = SkillActor.of(player)
        val context = SkillInvocation.builder(actor).data(mapOf("damage" to number, "list" to list)).build()
        number.set(9)
        list[0] = "after"
        assertEquals(4.0, context.data().getValue("damage").number())
        assertEquals("before", context.data().getValue("list").list().single().text())
        assertEquals(id, actor.id())
        assertSame(player, actor.player().orElseThrow())
        verify(exactly = 0) { player.name; player.health }
        val child = context.derive().data(mapOf("damage" to 8)).build()
        assertEquals(4.0, context.data().getValue("damage").number())
        assertEquals(8.0, child.data().getValue("damage").number())
        assertThrows(UnsupportedOperationException::class.java) { (context.data() as MutableMap).clear() }
    }

    @Test fun typedValuesRejectUnboundedOrLivePayloads() {
        assertThrows(IllegalArgumentException::class.java) { SkillValue.of(Double.NaN) }
        assertThrows(IllegalArgumentException::class.java) { SkillValue.of(mockk<LivingEntity>()) }
        assertThrows(IllegalArgumentException::class.java) { SkillValue.of("x".repeat(262145)) }
        val cyclic = mutableListOf<Any>()
        cyclic.add(cyclic)
        assertThrows(IllegalArgumentException::class.java) { SkillValue.of(cyclic) }
        var nested: Any = "leaf"
        repeat(65) { nested = listOf(nested) }
        assertThrows(IllegalArgumentException::class.java) { SkillValue.of(nested) }
        assertThrows(IllegalStateException::class.java) { SkillValue.of(true).number() }
    }

    @Test fun selectionsDeduplicateByIdentityWithoutLiveReads() {
        val id = UUID.randomUUID()
        val one = mockk<LivingEntity>()
        val otherReference = mockk<LivingEntity>()
        every { one.uniqueId } returns id
        every { otherReference.uniqueId } returns id
        val location = SkillLocation(UUID.randomUUID(), 1.0, 2.0, 3.0)
        val selection = SkillSelection(listOf(SkillActor.of(one), SkillActor.of(otherReference)), listOf(location, location))
        assertEquals(1, selection.entities().size)
        assertEquals(listOf(location), selection.locations())
        assertTrue(SkillActor.of(one).player().isEmpty)
        verify(exactly = 0) { one.location; one.isValid; otherReference.location }
    }
}
