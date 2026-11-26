package com.keopsfenks.arcana.skills.platform.providers

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.execution.PlatformFixture
import io.mockk.*
import org.bukkit.plugin.Plugin
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class ProviderLifecycleTests {
    @TempDir lateinit var directory: Path
    private fun owner() = mockk<Plugin>().also { every { it.name } returns "Alpha" }
    private fun fixture() = PlatformFixture(directory).also { f -> f.register("native_mark") { _, _ -> CompiledSkillMechanic { f.legacy.marks.add("mark"); f.success() } } }
    @Test fun disabledDefinitionsDoNotRequestExternalStatSnapshots() {
        val f = fixture(); val requests = AtomicInteger()
        f.platform.register(owner(), SkillComponentType.STAT, SkillKey("alpha", "stats"), object : SkillStatProvider {
            override fun descriptor() = SkillDescriptor.of("stats", "test")
            override fun snapshot(actor: SkillActor, keys: Set<SkillKey>) = CompletableFuture.completedFuture(mapOf(SkillKey("alpha", "power") to 7.0)).also { requests.incrementAndGet() }
        })
        f.write(options = "enabled: false\nbindings:\n  stat_power: {provider: alpha:stats, key: power}")
        f.load()
        assertEquals(SkillResult.Status.DISABLED, f.cast().join().status())
        assertEquals(0, requests.get())
    }
    @Test fun providerRetirementUsesTheSameGateAsTheAdmissionDecision() {
        val f = fixture()
        val handle = f.platform.register(owner(), SkillComponentType.RESOURCE, SkillKey("alpha", "external"), object : SkillResourceProvider {
            override fun descriptor() = SkillDescriptor.of("external", "test")
            override fun reserve(actor: SkillActor, amount: Double) = CompletableFuture.completedFuture(SkillResourceReservation.denied(SkillResult.of(SkillResult.Status.INSUFFICIENT_RESOURCE, "unused")))
        })
        val entered = CountDownLatch(1); val release = CountDownLatch(1); val closing = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val decision = pool.submit { f.engine.lifecycle.locked { entered.countDown(); check(release.await(3, TimeUnit.SECONDS)) } }
            assertTrue(entered.await(3, TimeUnit.SECONDS))
            val retirement = pool.submit { closing.countDown(); handle.close() }
            assertTrue(closing.await(3, TimeUnit.SECONDS))
            assertThrows(TimeoutException::class.java) { retirement.get(100, TimeUnit.MILLISECONDS) }
            assertTrue(handle.isActive)
            release.countDown(); decision.get(3, TimeUnit.SECONDS); retirement.get(3, TimeUnit.SECONDS)
            assertFalse(handle.isActive)
        } finally { release.countDown(); pool.shutdownNow() }
    }
    @Test fun lateReservationAfterProviderUnloadIsReleasedWithoutCommittingOrRunningEffects() {
        val f = fixture(); val owner = owner()
        val prepared = CompletableFuture<SkillResourceReservation>()
        val commits = AtomicInteger(); val releases = AtomicInteger()
        f.platform.register(owner, SkillComponentType.RESOURCE, SkillKey("alpha", "external"), object : SkillResourceProvider {
            override fun descriptor() = SkillDescriptor.of("external", "test")
            override fun reserve(actor: SkillActor, amount: Double) = prepared
        })
        f.write(options = "costs:\n  - {resource: alpha:external, amount: 2}"); f.load()
        val pending = f.cast(); assertFalse(pending.isDone)
        f.engine.closeOwner(owner)
        assertEquals(SkillResult.Status.CANCELLED, pending.join().status())
        prepared.complete(object : SkillResourceReservation {
            override fun commit() { commits.incrementAndGet() }
            override fun release() { releases.incrementAndGet() }
        })
        assertEquals(0, commits.get()); assertEquals(1, releases.get()); assertTrue(f.legacy.marks.isEmpty())
    }
    @Test fun declaredStatBindingsReplaceCallerDataAndRejectNonfiniteProviderValues() {
        val f = fixture(); val owner = owner(); var value = 7.0; var seen = -1.0
        f.platform.register(owner, SkillComponentType.STAT, SkillKey("alpha", "stats"), object : SkillStatProvider {
            override fun descriptor() = SkillDescriptor.of("stats", "test")
            override fun snapshot(actor: SkillActor, keys: Set<SkillKey>) = CompletableFuture.completedFuture(mapOf(SkillKey("alpha", "power") to value))
        })
        f.register("stat_mark") { _, _ -> CompiledSkillMechanic { seen = it.invocation().data().getValue("stat_power").number(); f.success() } }
        f.write(options = "bindings:\n  stat_power:\n    provider: alpha:stats\n    key: alpha:power", effects = "  - type: stat_mark")
        f.load()
        assertTrue(f.cast(input = f.invocation().derive().data(mapOf("stat_power" to 999)).build()).join().isSuccess)
        assertEquals(7.0, seen)
        value = Double.NaN
        assertEquals(SkillResult.Status.ERROR, f.cast().join().status())
        assertEquals(7.0, seen)
    }
}
