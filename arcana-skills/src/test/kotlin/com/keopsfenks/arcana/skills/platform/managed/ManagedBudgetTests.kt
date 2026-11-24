package com.keopsfenks.arcana.skills.platform.managed
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.execution.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ManagedBudgetTests {
    @TempDir lateinit var directory: Path
    @Test fun callback4097AndStep65537FailWithoutResettingTheInstanceBudget() {
        val f = PlatformFixture(directory); lateinit var managed: ManagedExecutionState
        f.register("adopt") { _, _ -> CompiledSkillMechanic { execution ->
            managed = ManagedExecutionState.adopt(execution as PlatformExecutionContext, execution.invocation().actor())
            f.success()
        } }
        f.write(effects = "  - type: adopt"); f.load(); f.cast().join()
        repeat(4096) { managed.callback() }; assertThrows(PlatformBudgetException::class.java) { managed.callback() }
        managed.close()
        f.cast().join(); repeat(65536) { managed.state.step() }; assertThrows(PlatformBudgetException::class.java) { managed.state.step() }
        managed.close(); assertEquals(0, f.platform.managed.size)
    }
    @Test fun explicitlyCancellingTheParentFutureRetiresItsAdoptedWork() {
        val f = PlatformFixture(directory); lateinit var managed: ManagedExecutionState
        f.register("adopt") { _, _ -> CompiledSkillMechanic { execution ->
            managed = ManagedExecutionState.adopt(execution as PlatformExecutionContext, execution.invocation().actor())
            execution.delay(10)
        } }
        f.write(effects = "  - type: adopt"); f.load(); val parent = f.cast()
        assertEquals(1, f.platform.managed.size); parent.cancel(false)
        assertEquals(0, f.platform.managed.size); assertTrue(managed.state.root.result.isDone)
    }
    @Test fun adoptedManagedDescendantsShareCountersAndSurviveNormalParentExpiry() {
        val f = PlatformFixture(directory); lateinit var parent: ManagedExecutionState; lateinit var child: ManagedExecutionState
        f.register("adopt") { _, _ -> CompiledSkillMechanic { execution ->
            parent = ManagedExecutionState.adopt(execution as PlatformExecutionContext, execution.invocation().actor())
            parent.execute(listOf(CompiledSkillMechanic { current ->
                child = ManagedExecutionState.adopt(current as PlatformExecutionContext, current.invocation().actor())
                f.success()
            })).thenApply { SkillResult.success() }
        } }
        f.write(effects = "  - type: adopt"); f.load(); f.cast().join()
        val steps = parent.state.root.appliedSteps; child.state.step()
        assertEquals(steps + 1, parent.state.root.appliedSteps)
        val callbacks = parent.callbackCount; child.callback(); assertEquals(callbacks + 1, parent.callbackCount)
        parent.remove(SkillRemovalReason.EXPIRED)
        assertTrue(child.lease.active); assertEquals(1, f.platform.managed.size)
        repeat(65536 - child.state.root.appliedSteps) { child.state.step() }
        assertThrows(PlatformBudgetException::class.java) { child.state.step() }
        child.close(); assertEquals(0, f.platform.managed.size)
    }
    @Test fun managedDescendantsCannotResetTheTwentyFourHourLineageDeadline() {
        val f = PlatformFixture(directory); lateinit var parent: ManagedExecutionState; lateinit var child: ManagedExecutionState
        f.register("adopt") { _, _ -> CompiledSkillMechanic { execution ->
            parent = ManagedExecutionState.adopt(execution as PlatformExecutionContext, execution.invocation().actor())
            f.success()
        } }
        f.write(effects = "  - type: adopt"); f.load(); f.cast().join()
        f.legacy.clock.set(86_399_950_000_000L)
        parent.execute(listOf(CompiledSkillMechanic { current -> child = ManagedExecutionState.adopt(current as PlatformExecutionContext, current.invocation().actor()); f.success() })).join()
        parent.remove(SkillRemovalReason.EXPIRED)
        assertTrue(child.lease.active)
        f.scheduler.advance(1); assertFalse(child.lease.active); assertEquals(0, f.platform.managed.size)
    }

    @Test fun detachedManagedFramesCopyInvocationVariablesAndKeepTheirOwnSequentialValues() {
        val f = PlatformFixture(directory); lateinit var managed: ManagedExecutionState; val key = SkillKey("arcana", "local")
        f.register("adopt") { _, _ -> CompiledSkillMechanic { execution ->
            val context = execution as PlatformExecutionContext
            context.variables.set(SkillVariableScope.INVOCATION, key, SkillValue.of(7), java.time.Duration.ZERO).toCompletableFuture().join()
            managed = ManagedExecutionState.adopt(context, context.invocation().actor()); f.success()
        } }
        f.write(effects = "  - type: adopt"); f.load(); f.cast().join()
        managed.execute(listOf(CompiledSkillMechanic { current ->
            val variables = current.services().require(SkillVariables::class.java)
            assertEquals(7.0, variables.get(SkillVariableScope.INVOCATION, key).orElseThrow().number())
            variables.set(SkillVariableScope.INVOCATION, key, SkillValue.of(9), java.time.Duration.ZERO).thenApply { SkillResult.success() }
        })).join().also { assertTrue(it.isSuccess, it.message()) }
        managed.execute(listOf(CompiledSkillMechanic { current ->
            assertEquals(9.0, current.services().require(SkillVariables::class.java).get(SkillVariableScope.INVOCATION, key).orElseThrow().number()); f.success()
        })).join().also { assertTrue(it.isSuccess, it.message()) }
        managed.close()
    }

    @Test fun cancellingAPendingParallelFrameRetiresItsAdoptedWorkWhileTheRootKeepsWaiting() {
        val f = PlatformFixture(directory); lateinit var managed: ManagedExecutionState
        f.register("parallel_adopt") { _, _ -> CompiledSkillMechanic { execution ->
            val context = execution as PlatformExecutionContext
            val branch = listOf(CompiledSkillMechanic { child -> managed = ManagedExecutionState.adopt(child as PlatformExecutionContext, child.invocation().actor()); child.delay(50) })
            val failure = listOf(CompiledSkillMechanic { child -> child.delay(1).thenApply { SkillResult.of(SkillResult.Status.ERROR, "expected") } })
            context.services().require(ParallelScope::class.java).execute(listOf(branch, failure)).thenCompose { context.delay(100) }
        } }
        f.write(effects = "  - type: parallel_adopt"); f.load(); val root = f.cast()
        assertEquals(1, f.platform.managed.size)
        f.scheduler.advance(1); assertFalse(root.isDone)
        assertEquals(0, f.platform.managed.size); assertTrue(managed.state.root.result.isDone)
        f.engine.cancel(f.legacy.playerId)
    }

    @Test fun aCancelledOutcomeAlsoAbortsAdoptionsAfterTheirNodeScopeHasFinished() {
        val f = PlatformFixture(directory)
        f.register("adopt") { _, _ -> CompiledSkillMechanic { execution ->
            ManagedExecutionState.adopt(execution as PlatformExecutionContext, execution.invocation().actor()); f.success()
        } }
        f.register("cancel") { _, _ -> CompiledSkillMechanic { java.util.concurrent.CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.CANCELLED, "expected")) } }
        f.write(effects = "  - type: adopt\n    repeat: 1\n  - type: cancel"); f.load()
        assertEquals(SkillResult.Status.CANCELLED, f.cast().join().status()); assertEquals(0, f.platform.managed.size)
    }

    @Test fun disabledOwnerCannotRunAQueuedManagedHookBeforeItsHandlesFinishClosing() {
        val f = PlatformFixture(directory); lateinit var managed: ManagedExecutionState; var calls = 0
        f.register("adopt") { _, _ -> CompiledSkillMechanic { execution ->
            managed = ManagedExecutionState.adopt(execution as PlatformExecutionContext, execution.invocation().actor()); f.success()
        } }
        f.write(effects = "  - type: adopt"); f.load(); f.cast().join()
        f.engine.lifecycle.disable(f.legacy.plugin) { emptyList() }
        assertFalse(managed.lease.active)
        val result = managed.execute(listOf(CompiledSkillMechanic { calls++; f.success() })).join()
        assertEquals(SkillResult.Status.DEPENDENCY_UNAVAILABLE, result.status()); assertEquals(0, calls)
        f.engine.closeOwner(f.legacy.plugin); assertEquals(0, f.platform.managed.size)
    }

}
