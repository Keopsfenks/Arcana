package com.keopsfenks.arcana.skills.platform.admission

import com.keopsfenks.arcana.core.skills.platform.SkillKey
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong

class ResourceReservationTests {
    @Test fun onlyOneConcurrentLastChargeReservationWinsAndReleasingRestoresThePermit() {
        val now = AtomicLong()
        val store = ChargeStore(now::get)
        val actor = UUID.randomUUID(); val group = SkillKey("arcana", "shared")
        val options = ChargeOptions(1, 1_000_000_000)
        val pool = Executors.newFixedThreadPool(8)
        try {
            val winners = pool.invokeAll(List(32) { Callable { store.reserve(actor, group, options) } }).mapNotNull { it.get() }
            assertEquals(1, winners.size)
            winners.single().release(); winners.single().release()
            assertEquals(1, store.available(actor, group, options))
            val accepted = store.reserve(actor, group, options)!!
            assertTrue(accepted.commit()); assertTrue(accepted.commit())
            assertNull(store.reserve(actor, group, options))
            now.set(1_000_000_000)
            assertNotNull(store.reserve(actor, group, options))
        } finally { pool.shutdownNow() }
    }
    @Test fun refillUsesMonotonicWrapAndLogoutInvalidatesPendingReservations() {
        val now = AtomicLong(Long.MAX_VALUE - 5)
        val store = ChargeStore(now::get)
        val actor = UUID.randomUUID(); val group = SkillKey("arcana", "charge")
        val options = ChargeOptions(1, 10)
        assertTrue(store.reserve(actor, group, options)!!.commit())
        now.set(Long.MIN_VALUE + 5)
        val pending = store.reserve(actor, group, options)!!
        store.remove(actor)
        assertFalse(pending.commit())
        assertEquals(1, store.available(actor, group, options))
    }
}
