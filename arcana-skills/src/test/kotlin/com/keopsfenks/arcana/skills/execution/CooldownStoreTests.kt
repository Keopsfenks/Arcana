package com.keopsfenks.arcana.skills.execution

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

class CooldownStoreTests {
    @Test fun `pending permits block shared legacy groups until committed or released`() {
        val now = java.util.concurrent.atomic.AtomicLong()
        val store = CooldownStore(now::get)
        val actor = UUID.randomUUID()
        val pending = store.reserve(actor, "arcana:shared", 1_000_000_000)!!
        assertFalse(store.tryAcquire(actor, "shared", 0))
        pending.release(); pending.release()
        val accepted = store.reserve(actor, "shared", 1_000_000_000)!!
        now.set(5_000_000_000)
        assertTrue(accepted.commit()); assertTrue(accepted.commit())
        assertEquals(1_000_000_000, store.remaining(actor, "arcana:shared"))
        now.set(6_000_000_000)
        val stale = store.reserve(actor, "shared", 0)!!
        store.remove(actor)
        assertFalse(stale.commit())
    }
    @Test fun `zero duration still respects an active shared cooldown`() {
        val now = AtomicLong(0)
        val store = CooldownStore(now::get)
        val player = UUID.randomUUID()
        assertTrue(store.tryAcquire(player, "shared", 1000))
        assertFalse(store.tryAcquire(player, "shared", 0))
        now.set(1000)
        assertTrue(store.tryAcquire(player, "shared", 0))
        assertEquals(0, store.remaining(player, "shared"))
    }

    @Test fun `one concurrent admission wins for the same player and group`() {
        val now = AtomicLong(0)
        val store = CooldownStore(now::get)
        val player = UUID.randomUUID()
        val pool = Executors.newFixedThreadPool(8)
        try {
            val accepted = pool.invokeAll(List(64) { Callable { store.tryAcquire(player, "shared", 1000) } }).count { it.get() }
            assertEquals(1, accepted)
            assertTrue(store.tryAcquire(UUID.randomUUID(), "shared", 1000))
            assertTrue(store.tryAcquire(player, "other", 1000))
            now.set(1000)
            assertTrue(store.tryAcquire(player, "shared", 1000))
        } finally { pool.shutdownNow() }
    }

    @Test fun `expiry uses injected monotonic time and logout clears only that player`() {
        val now = AtomicLong(Long.MAX_VALUE - 10)
        val store = CooldownStore(now::get)
        val one = UUID.randomUUID()
        val two = UUID.randomUUID()
        assertTrue(store.tryAcquire(one, "group", 20))
        assertTrue(store.tryAcquire(two, "group", 20))
        assertEquals(20, store.remaining(one, "group"))
        now.set(Long.MIN_VALUE + 10)
        assertTrue(store.tryAcquire(one, "group", 20))
        store.remove(one)
        assertEquals(0, store.remaining(one, "group"))
        assertTrue(store.remaining(two, "group") <= 0)
    }
}
