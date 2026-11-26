package com.keopsfenks.arcana.skills.platform.holders
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.execution.PlatformFixture
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

class StatContributionTests {
    @TempDir lateinit var directory: Path
    @Test fun lateNativeLeaseIsClosedOnceAndDoesNotPublishStatsAfterOwnerRemoval() {
        val f = PlatformFixture(directory); val pending = CompletableFuture<SkillContributionLease>(); val closes = AtomicInteger()
        f.register("native_mark") { _, _ -> CompiledSkillMechanic { f.success() } }
        f.platform.register(f.legacy.plugin, SkillComponentType.CONTRIBUTION, SkillKey("arcana", "pending"), object : SkillContribution(SkillDescriptor.of("pending", "test")) {
            override fun compile(arguments: SkillArguments, compilation: SkillCompilation) = CompiledSkillContribution { _, _ -> pending }
        })
        val provider = f.platform.register(f.legacy.plugin, SkillComponentType.HOLDER, SkillKey("arcana", "slots"), object : SkillHolderProvider {
            override fun descriptor() = SkillDescriptor.of("slots", "test")
            override fun holders(actor: SkillActor) = CompletableFuture.completedFuture(listOf(SkillHolder(SkillKey("arcana", "ring"), "main", listOf(SkillKey("arcana", "native")), emptyMap())))
        })
        f.write(options = "activation: holder\npassives: [{type: pending}]"); f.load()
        val actor = f.invocation().actor(); val refresh = f.platform.refreshHolders(actor).toCompletableFuture()
        provider.close()
        pending.complete(object : SkillContributionLease {
            override fun stats() = mapOf(SkillKey("arcana", "power") to 5.0)
            override fun close() { closes.incrementAndGet() }
        })
        assertTrue(refresh.isDone); assertEquals(1, closes.get()); assertTrue(f.platform.contributions.snapshot(actor).isEmpty())
        assertEquals(0, f.platform.managed.size)
    }
    @Test fun nativeStatProviderReturnsPureFiniteRequestedSumsWithoutLiveEntityReads() {
        val f = PlatformFixture(directory)
        val provider = com.keopsfenks.arcana.skills.components.providers.ContributionStatProvider(f.platform.contributions)
        val actor = f.invocation().actor(); val key = SkillKey("arcana", "absent")
        assertTrue(provider.supportsLocal())
        assertEquals(mapOf(key to 0.0), provider.snapshotLocal(actor, setOf(key)))
        assertEquals(mapOf(key to 0.0), provider.snapshot(actor, setOf(key)).join())
    }

}
