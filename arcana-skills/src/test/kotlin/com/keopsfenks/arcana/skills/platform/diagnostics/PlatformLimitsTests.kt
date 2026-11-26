package com.keopsfenks.arcana.skills.platform.diagnostics
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.execution.*
import com.keopsfenks.arcana.skills.execution.ScheduledPayload
import io.mockk.*
import org.bukkit.entity.LivingEntity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CompletableFuture

class PlatformLimitsTests {
    @Test fun configuredManagedLimitsCanOnlyLowerHardCaps(@TempDir directory: Path) {
        val f = PlatformFixture(directory)
        val lower = com.keopsfenks.arcana.skills.platform.managed.ManagedLimits(actorInstances = 1, engineInstances = 2, callbacks = 2, steps = 3, lifetimeTicks = 2)
        f.platform.managed.configure(lower)
        lateinit var adopted: com.keopsfenks.arcana.skills.platform.managed.ManagedExecutionState
        f.register("native_mark") { _, _ -> CompiledSkillMechanic { context -> adopted = com.keopsfenks.arcana.skills.platform.managed.ManagedExecutionState.adopt(context as PlatformExecutionContext, context.invocation().actor()); f.success() } }
        f.write(); f.load(); f.cast().join(); repeat(2) { adopted.callback() }
        assertThrows(PlatformBudgetException::class.java) { adopted.callback() }; assertEquals(3, adopted.state.root.stepLimit)
        f.scheduler.advance(2); assertFalse(adopted.lease.active); assertEquals(0, f.platform.managed.size)
        assertThrows(IllegalArgumentException::class.java) { com.keopsfenks.arcana.skills.platform.managed.ManagedLimits(actorInstances = 65) }
        assertThrows(IllegalArgumentException::class.java) { com.keopsfenks.arcana.skills.platform.managed.ManagedLimits(engineInstances = 4097) }
        f.engine.shutdown()
    }
    @Test fun sixteenConcurrentBranchesShareOneCounterAndReleaseIt(@TempDir directory: Path) {
        val f = PlatformFixture(directory); val root = com.keopsfenks.arcana.skills.execution.ExecutionState(f.legacy.playerId, f.platform.repository.snapshot().legacy)
        val state = PlatformExecutionState(root, f.platform.repository.snapshot()); val held = state.reserveBranches(16)
        assertThrows(PlatformBudgetException::class.java) { state.reserveBranches(1) }; held.close(); held.close(); state.reserveBranches(16).close()
        assertThrows(PlatformBudgetException::class.java) { state.reserveBranches(17) }; root.result.complete(com.keopsfenks.arcana.core.skills.SkillResult.success()); f.engine.shutdown()
        println("load: simultaneousBranches=16 overflowRejected=true permitsReleased=true")
    }
    @Test fun engineAndActorLimitsRelease4096LeasesExactlyOnce(@TempDir directory: Path) {
        val f = PlatformFixture(directory); val owner = f.register("native_mark") { _, _ -> CompiledSkillMechanic { f.success() } }
        val registry = f.platform.managed; val memory = Runtime.getRuntime(); val before = memory.totalMemory() - memory.freeMemory()
        val actors = (1..65).map { val entity = mockk<LivingEntity>(); every { entity.uniqueId } returns UUID.randomUUID(); SkillActor.of(entity) }
        val leases = (0 until 4096).map { registry.reserve(owner, actors[it / 64]) }; var cleanups = 0; leases.forEach { it.onClose { cleanups++ } }
        assertThrows(PlatformBudgetException::class.java) { registry.reserve(owner, actors[64]) }
        assertThrows(PlatformBudgetException::class.java) { registry.reserve(owner, actors[0]) }
        val allocated = memory.totalMemory() - memory.freeMemory() - before
        registry.close(); registry.close(); assertEquals(4096, cleanups); assertEquals(0, registry.size)
        println("load: managed=4096 actors=64 cleanups=$cleanups retainedRegistry=${registry.size} heapDeltaBytes=$allocated (non-GC estimate)")
        f.engine.shutdown()
    }
    @Test fun cancelledHostCallbacksReleaseTheirActionBeforeTheNativeDeadline() {
        val future = CompletableFuture<Unit>(); var calls = 0
        val nativeQueued = ScheduledPayload(future) { calls++ }
        assertTrue(nativeQueued.pending); future.cancel(false); assertFalse(nativeQueued.pending)
        nativeQueued.run(); assertEquals(0, calls)
    }
}
