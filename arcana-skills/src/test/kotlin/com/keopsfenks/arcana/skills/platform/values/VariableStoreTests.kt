package com.keopsfenks.arcana.skills.platform.values

import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.core.data.Profile
import com.keopsfenks.arcana.core.data.keys.PersistentDataKey
import io.mockk.every
import io.mockk.mockk
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong
import java.util.function.UnaryOperator

class VariableStoreTests {
    @Test fun persistedKeysKeepNamespaceAndLocalNameBoundaries() {
        assertNotEquals(PersistentVariableStore.dataKey(SkillKey("a_b", "c"), false),
            PersistentVariableStore.dataKey(SkillKey("a", "b_c"), false))
        assertNotEquals(PersistentVariableStore.dataKey(SkillKey("a", "b"), false),
            PersistentVariableStore.dataKey(SkillKey("a", "b"), true))
    }
    private fun frame(id: UUID = UUID.randomUUID()): InvocationFrame {
        val player = mockk<Player>()
        every { player.uniqueId } returns id
        return InvocationFrame(SkillInvocation.builder(SkillActor.of(player)).build())
    }

    @Test fun concurrentActorAddsAreAtomicAndServerScopesAreIndependent() {
        val store = VariableStore()
        val one = frame()
        val access = store.access(one)
        val key = SkillKey("arcana", "counter")
        val pool = Executors.newFixedThreadPool(8)
        try {
            val jobs = List(128) { Callable { access.add(SkillVariableScope.ACTOR, key, 1.0).toCompletableFuture().join() } }
            pool.invokeAll(jobs).forEach { it.get() }
            assertEquals(128.0, access.get(SkillVariableScope.ACTOR, key).orElseThrow().number())
            access.set(SkillVariableScope.SERVER, key, SkillValue.of(7), Duration.ZERO).toCompletableFuture().join()
            assertTrue(VariableStore().access(one).get(SkillVariableScope.SERVER, key).isEmpty)
            assertTrue(access.remove(SkillVariableScope.ACTOR, key).toCompletableFuture().join())
            assertFalse(access.remove(SkillVariableScope.ACTOR, key).toCompletableFuture().join())
        } finally { pool.shutdownNow() }
    }

    @Test fun expiryUsesMonotonicTimeAcrossWrapAndOfflineViewsCannotResurrectState() {
        val now = AtomicLong(Long.MAX_VALUE - 10)
        val store = VariableStore(now::get, { 0L })
        val invocation = frame()
        val access = store.access(invocation)
        val key = SkillKey("arcana", "counter")
        access.set(SkillVariableScope.ACTOR, key, SkillValue.of(3), Duration.ofNanos(20)).toCompletableFuture().join()
        now.set(Long.MIN_VALUE + 10)
        assertTrue(access.get(SkillVariableScope.ACTOR, key).isEmpty)
        access.set(SkillVariableScope.ACTOR, key, SkillValue.of(4), Duration.ZERO).toCompletableFuture().join()
        store.evictActor(invocation.invocation.actor().id())
        assertEquals(0, store.entryCount(invocation.invocation.actor().id()))
        assertThrows(CancellationException::class.java) { access.get(SkillVariableScope.ACTOR, key) }
    }

    private class MemoryProfile : Profile {
        val values = ConcurrentHashMap<NamespacedKey, Any>()
        @Volatile var failReads = false
        @Suppress("UNCHECKED_CAST") override fun <T : Any> read(key: PersistentDataKey<T>): T {
            check(!failReads) { "temporary profile failure" }
            return (values[key.key] ?: key.defaultValue) as T
        }
        override fun <T : Any> write(key: PersistentDataKey<T>, value: T) { values[key.key] = value }
    }

    private fun port(profile: MemoryProfile, executor: Executor = Executor { it.run() }): PersistentVariableStore {
        val keys = ConcurrentHashMap<String, PersistentDataKey<String>>()
        return PersistentVariableStore(profile = { profile }, keys = { key, local ->
            keys.computeIfAbsent("$local:$key") {
                mockk<PersistentDataKey<String>>().also {
                    every { it.key } returns NamespacedKey("arcana", "${if (local) "s" else "p"}_${key.namespace()}_${key.value()}")
                    every { it.defaultValue } returns ""
                }
            }
        }, executor = executor)
    }

    @Test fun optInPersistenceRoundTripsThroughArcanaProfilesWithoutSerializingBukkitObjects() {
        val profile = MemoryProfile()
        val port = port(profile)
        val key = SkillKey("arcana", "saved")
        val invocation = frame()
        val store = VariableStore(persistence = port)
        val access = store.access(invocation, mapOf(SkillVariableScope.ACTOR to setOf(key)))
        access.prepare().join()
        access.set(SkillVariableScope.ACTOR, key, SkillValue.of(mapOf("number" to 7, "text" to "<red>literal")), Duration.ZERO).toCompletableFuture().join()
        store.evictActor(invocation.invocation.actor().id())
        val restored = VariableStore(persistence = port).access(frame(invocation.invocation.actor().id()), mapOf(SkillVariableScope.ACTOR to setOf(key)))
        restored.prepare().join()
        val value = restored.get(SkillVariableScope.ACTOR, key).orElseThrow().map()
        assertEquals(7.0, value.getValue("number").number())
        assertEquals("<red>literal", value.getValue("text").text())
        assertEquals(1, profile.values.size)
        port.close()
    }

    @Test fun sharedPrefetchSurvivesTheFirstCallersCancellationAndFailedReadsCanRetry() {
        val profile = MemoryProfile()
        val queued = java.util.ArrayDeque<Runnable>()
        val port = port(profile, Executor { queued.add(it) })
        val store = VariableStore(persistence = port)
        val key = SkillKey("arcana", "saved")
        val one = frame()
        val two = frame(one.invocation.actor().id())
        val first = store.access(one, mapOf(SkillVariableScope.ACTOR to setOf(key))).prepare()
        val secondAccess = store.access(two, mapOf(SkillVariableScope.ACTOR to setOf(key)))
        val second = secondAccess.prepare()
        one.close()
        while (queued.isNotEmpty()) queued.removeFirst().run()
        assertTrue(first.isCompletedExceptionally)
        assertDoesNotThrow { second.join() }
        assertTrue(secondAccess.get(SkillVariableScope.ACTOR, key).isEmpty)
        store.close()

        val failingProfile = MemoryProfile().apply { failReads = true }
        val retryStore = VariableStore(persistence = port(failingProfile))
        val access = retryStore.access(frame(), mapOf(SkillVariableScope.ACTOR to setOf(key)))
        assertThrows(CompletionException::class.java) { access.prepare().join() }
        failingProfile.failReads = false
        assertDoesNotThrow { access.prepare().join() }
        retryStore.close()
    }

    @Test fun persistenceMatchesAtomicMemoryStateAfterConcurrentSetAddAndRemove() {
        val profile = MemoryProfile()
        val port = port(profile)
        val store = VariableStore(persistence = port)
        val invocation = frame()
        val key = SkillKey("arcana", "counter")
        val view = store.access(invocation, mapOf(SkillVariableScope.ACTOR to setOf(key)))
        view.prepare().join()
        val pool = Executors.newFixedThreadPool(8)
        try {
            repeat(4) {
                pool.invokeAll(List(128) { index -> Callable {
                    when (index % 3) {
                        0 -> view.set(SkillVariableScope.ACTOR, key, SkillValue.of(index), Duration.ZERO).toCompletableFuture().join()
                        1 -> view.add(SkillVariableScope.ACTOR, key, 1.0).toCompletableFuture().join()
                        else -> view.remove(SkillVariableScope.ACTOR, key).toCompletableFuture().join()
                    }
                } }).forEach { it.get() }
                val expected = view.get(SkillVariableScope.ACTOR, key)
                val restored = VariableStore(persistence = port).access(frame(invocation.invocation.actor().id()), mapOf(SkillVariableScope.ACTOR to setOf(key)))
                restored.prepare().join()
                assertEquals(expected, restored.get(SkillVariableScope.ACTOR, key))
            }
        } finally { pool.shutdownNow(); store.close() }
    }
}
