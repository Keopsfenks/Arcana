package com.keopsfenks.arcana.skills.platform.execution

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class PlatformLimitsTests {
    @TempDir lateinit var directory: Path
    @Test fun legacyExecutionRetainedFromACompletedBranchCannotActWhileItsRootIsStillActive() {
        val f = PlatformFixture(directory)
        var retained: SkillExecution? = null
        var applied = 0
        f.legacy.register("retain") { _, _ -> CompiledSkillEffect { retained = it; f.success() } }
        f.legacy.write("old", effects = "  - type: retain")
        f.register("native_mark") { _, _ -> CompiledSkillMechanic { execution ->
            ParallelScope(execution as PlatformExecutionContext).execute(listOf(listOf(CompiledSkillMechanic { it.cast(SkillKey("arcana", "old"), emptyMap()) })))
                .thenCompose { execution.delay(100) }
        } }
        f.write(); f.load(); val root = f.cast()
        assertFalse(root.isDone)
        val rejected = retained!!.onTarget(SkillTarget.CASTER) { applied++; SkillResult.success() }.handle { result, error -> error != null || !result.isSuccess }.toCompletableFuture().join()
        assertTrue(rejected)
        assertEquals(0, applied)
        f.engine.cancel(f.legacy.playerId)
    }
    @Test fun aggregatedOrdinaryFailuresCannotHideHardFamilyBudgetFailures() {
        val outcome = ParallelScope.aggregate(listOf(SkillResult.of(SkillResult.Status.NO_TARGET, "one target retired"), SkillResult.of(SkillResult.Status.BUDGET_EXCEEDED, "limit")))
        assertEquals(SkillResult.Status.BUDGET_EXCEEDED, outcome.status())
    }
    @Test fun parallelResultsRetainInputOrderAndThe17thBranchIsRejected() {
        val f = PlatformFixture(directory)
        var results = emptyList<SkillResult>()
        f.register("native_mark") { _, _ -> CompiledSkillMechanic { execution ->
            val context = execution as PlatformExecutionContext
            ParallelScope(context).execute(listOf(3L, 1L, 2L).map { ticks -> listOf(CompiledSkillMechanic {
                it.delay(ticks).thenApply { SkillResult(SkillResult.Status.SUCCESS, ticks.toString()) }
            }) }).thenApply { results = it; SkillResult.success() }
        } }
        f.write(); f.load()
        val pending = f.cast()
        f.scheduler.advance(1); assertFalse(pending.isDone)
        f.scheduler.advance(2); assertTrue(pending.join().isSuccess)
        assertEquals(listOf("3", "1", "2"), results.map { it.message() })
        f.register("too_many") { _, _ -> CompiledSkillMechanic { execution ->
            ParallelScope(execution as PlatformExecutionContext).execute(List(17) { emptyList() }).thenApply { SkillResult.success() }
        } }
        f.write("too_many", effects = "  - type: too_many"); f.load()
        assertEquals(SkillResult.Status.BUDGET_EXCEEDED, f.cast("too_many").join().status())
    }
    @Test fun failFastCancelsPendingSiblingWorkEvenWhenTheRootContinuesWaiting() {
        val f = PlatformFixture(directory)
        var applied = 0
        var active: PlatformExecutionContext? = null
        f.register("native_mark") { _, _ -> CompiledSkillMechanic { execution ->
            val context = execution as PlatformExecutionContext
            active = context
            ParallelScope(context).execute(listOf(
                listOf(CompiledSkillMechanic { it.delay(1).thenApply { SkillResult.of(SkillResult.Status.ERROR, "first failed") } }),
                listOf(CompiledSkillMechanic { it.delay(20).thenApply { applied++; SkillResult.success() } })
            ), failFast = true).thenCompose { execution.delay(100) }
        } }
        f.write(); f.load(); val pending = f.cast()
        f.scheduler.advance(1)
        assertFalse(pending.isDone)
        assertEquals(1, active!!.state.root.pendingWork, "The canceled sibling still owns scheduled work")
        f.scheduler.advance(20)
        assertEquals(0, applied)
        f.engine.cancel(f.legacy.playerId)
        assertEquals(SkillResult.Status.CANCELLED, pending.join().status())
    }
    @Test fun parallelChildrenShareTheSame1024StepCounter() {
        val f = PlatformFixture(directory)
        var applied = 0
        f.register("native_mark") { _, _ -> CompiledSkillMechanic { execution ->
            ParallelScope(execution as PlatformExecutionContext).execute(List(2) {
                List(600) { CompiledSkillMechanic { applied++; f.success() } }
            }, failFast = false).thenApply { results -> results.firstOrNull { !it.isSuccess } ?: SkillResult.success() }
        } }
        f.write(); f.load()
        assertEquals(SkillResult.Status.BUDGET_EXCEEDED, f.cast().join().status())
        assertEquals(1023, applied)
    }
    @Test fun aNativeToLegacyCallCannotResetTheRootsStepBudget() {
        val fixture = PlatformFixture(directory)
        var applied = 0
        fixture.legacy.register("many") { _, _ -> CompiledSkillEffect { execution ->
            execution.execute(List(1024) { CompiledSkillEffect { applied++; fixture.success() } })
        } }
        fixture.legacy.write("old", effects = "  - type: many")
        fixture.register("native_mark") { _, _ -> CompiledSkillMechanic { it.cast(SkillKey("arcana", "old"), emptyMap()) } }
        fixture.write(); fixture.load()
        assertEquals(SkillResult.Status.BUDGET_EXCEEDED, fixture.cast().join().status())
        assertEquals(1022, applied)
    }
    @Test fun nestedNativeListsShareTheRoots1024AppliedSteps() {
        val fixture = PlatformFixture(directory)
        var applied = 0
        fixture.register("native_mark") { _, _ -> CompiledSkillMechanic { execution ->
            execution.execute(List(1024) { CompiledSkillMechanic { applied++; fixture.success() } })
        } }
        fixture.write(); fixture.load()
        assertEquals(SkillResult.Status.BUDGET_EXCEEDED, fixture.cast().join().status())
        assertEquals(1023, applied)
    }
    @Test fun recursiveCastsStopAt16RatherThanReenterAsFreshRoots() {
        val fixture = PlatformFixture(directory)
        var calls = 0
        fixture.register("native_mark") { _, _ -> CompiledSkillMechanic { calls++; it.cast(SkillKey("arcana", "native"), emptyMap()) } }
        fixture.write(); fixture.load()
        assertEquals(SkillResult.Status.BUDGET_EXCEEDED, fixture.cast().join().status())
        assertEquals(16, calls)
    }
}
