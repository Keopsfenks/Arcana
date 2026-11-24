package com.keopsfenks.arcana.skills.platform.auras
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.execution.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

class AuraLifecycleTests {
    @TempDir lateinit var directory: Path
    @Test fun adoptedAuraSurvivesParentSuccessAndExpiresWithOneEndAndCleanup() {
        val f = PlatformFixture(directory); val marks = mutableListOf<String>(); var cleanups = 0; var handle: SkillManagedHandle? = null
        fun hook(mark: String) = CompiledSkillMechanic { marks.add(mark); f.success() }
        f.register("apply_aura") { _, _ -> CompiledSkillMechanic { execution ->
            val context = execution as PlatformExecutionContext; val revision = context.state.snapshot.entries.getValue(context.definition.key)
            val component = CompiledSkillMechanic { current ->
                current.services().require(com.keopsfenks.arcana.skills.platform.managed.ManagedExecutionState::class.java).own(AutoCloseable { cleanups++ }); f.success()
            }
            val definition = AuraDefinition(SkillKey("arcana", "buff"), revision, listOf(hook("start")), listOf(hook("tick")), listOf(hook("end")), components = listOf(component))
            f.platform.auras.apply(definition, context.invocation(), AuraOptions(5, 2), context).thenApply { value ->
                handle = value; SkillResult.success()
            }
        } }
        f.write(effects = "  - type: apply_aura"); f.load(); assertTrue(f.cast().join().isSuccess)
        assertTrue(handle!!.active()); assertEquals(listOf("start"), marks)
        f.scheduler.advance(2); f.scheduler.advance(2); assertEquals(listOf("start", "tick", "tick"), marks)
        f.scheduler.advance(1); assertFalse(handle!!.active()); assertEquals(SkillRemovalReason.EXPIRED, handle!!.reason().orElseThrow())
        assertEquals(listOf("start", "tick", "tick", "end"), marks); assertEquals(1, cleanups)
        handle!!.remove(SkillRemovalReason.DISPELLED); f.scheduler.advance(20); assertEquals(1, cleanups); assertEquals(0, f.platform.managed.size)
    }
    @Test fun explicitActorCancellationAndDefinitionReloadNeverRunInvalidatedEndHooks() {
        val f = PlatformFixture(directory); var ends = 0; val handles = mutableListOf<SkillManagedHandle>()
        f.register("apply_aura") { _, _ -> CompiledSkillMechanic { execution ->
            val context = execution as PlatformExecutionContext; val revision = context.state.snapshot.entries.getValue(context.definition.key)
            f.platform.auras.apply(AuraDefinition(SkillKey("arcana", "buff"), revision, onEnd = listOf(CompiledSkillMechanic { ends++; f.success() })), context.invocation(), AuraOptions(100, 10), context)
                .thenApply { handles.add(it); SkillResult.success() }
        } }
        f.write(effects = "  - type: apply_aura"); f.load(); f.cast().join()
        f.engine.cancel(f.legacy.playerId); assertFalse(handles.last().active()); assertEquals(0, ends)
        f.cast().join(); f.load(); assertFalse(handles.last().active()); f.scheduler.advance(100); assertEquals(0, ends); assertEquals(0, f.platform.managed.size)
    }
    @Test fun refreshStacksCasterSharingOverwriteAndChargesUseOneOwnedLifetime() {
        val f = PlatformFixture(directory); var starts = 0; var ends = 0; var options = AuraOptions(10, 2, stackLimit = 2); val handles = mutableListOf<SkillManagedHandle>()
        f.register("apply_aura") { _, _ -> CompiledSkillMechanic { execution ->
            val context = execution as PlatformExecutionContext; val revision = context.state.snapshot.entries.getValue(context.definition.key)
            val definition = AuraDefinition(SkillKey("arcana", "buff"), revision, onStart = listOf(CompiledSkillMechanic { starts++; f.success() }), onEnd = listOf(CompiledSkillMechanic { ends++; f.success() }))
            f.platform.auras.apply(definition, context.invocation(), options, context).thenApply { handles.add(it); SkillResult.success() }
        } }
        f.write(effects = "  - type: apply_aura"); f.load(); f.cast().join(); f.scheduler.advance(4); f.cast().join()
        assertSame(handles[0], handles[1]); assertEquals(2, (handles[0] as AuraInstance).stacks); assertEquals(1, starts)
        f.scheduler.advance(6); assertTrue(handles[0].active())
        options = options.copy(overwrite = true); f.cast().join(); assertNotEquals(handles[0].id(), handles.last().id()); assertEquals(2, starts); assertEquals(1, ends)
        (handles.last() as AuraInstance).addCharges(-1).join(); assertFalse(handles.last().active()); assertEquals(SkillRemovalReason.CHARGES_EXHAUSTED, handles.last().reason().orElseThrow()); assertEquals(2, ends)
    }
    @Test fun hardLifetimeClosesAnAsynchronousEndHookAndReportsOwnerAndEngineRemovalSeparately() {
        val f = PlatformFixture(directory); var handle: SkillManagedHandle? = null; var cleanup = 0; var late = 0
        f.register("apply_aura") { _, _ -> CompiledSkillMechanic { execution ->
            val context = execution as PlatformExecutionContext; val revision = context.state.snapshot.entries.getValue(context.definition.key)
            val onEnd = listOf(CompiledSkillMechanic { current -> current.delay(1_728_000).thenApply { late++; SkillResult.success() } })
            f.platform.auras.apply(AuraDefinition(SkillKey("arcana", "buff"), revision, onEnd = onEnd), context.invocation(), AuraOptions(1, 1_728_000), context).thenApply { value ->
                handle = value; (value as AuraInstance).managed.own(AutoCloseable { cleanup++ }); SkillResult.success()
            }
        } }
        f.write(effects = "  - type: apply_aura"); f.load(); f.cast().join()
        f.scheduler.advance(1); assertEquals(1, f.platform.managed.size); assertEquals(1, cleanup)
        f.scheduler.advance(1_727_999); assertEquals(0, f.platform.managed.size); assertEquals(1, cleanup); assertEquals(0, late)
        f.cast().join(); f.engine.closeOwner(f.legacy.plugin)
        assertEquals(SkillRemovalReason.OWNER_UNLOADED, handle!!.reason().orElseThrow()); assertEquals(2, cleanup)
        f.engine.enableOwner(f.legacy.plugin)
    }
    @Test fun engineShutdownAndReloadHaveTheirOwnTerminalReasons() {
        val f = PlatformFixture(directory); val handles = mutableListOf<SkillManagedHandle>()
        f.register("apply_aura") { _, _ -> CompiledSkillMechanic { execution ->
            val context = execution as PlatformExecutionContext; val revision = context.state.snapshot.entries.getValue(context.definition.key)
            f.platform.auras.apply(AuraDefinition(SkillKey("arcana", "buff"), revision), context.invocation(), AuraOptions(100), context).thenApply { handles.add(it); SkillResult.success() }
        } }
        f.write(effects = "  - type: apply_aura"); f.load(); f.cast().join(); f.load()
        assertEquals(SkillRemovalReason.DEFINITION_RELOADED, handles.last().reason().orElseThrow())
        f.cast().join(); f.engine.shutdown(); assertEquals(SkillRemovalReason.ENGINE_STOPPED, handles.last().reason().orElseThrow())
    }

    @Test fun distinctCastersAreIsolatedUnlessSharingIsExplicitAndTargetRetirementCleansBoth() {
        val f = PlatformFixture(directory); val handles = mutableListOf<SkillManagedHandle>(); var sharing = false
        f.register("apply_aura") { _, _ -> CompiledSkillMechanic { execution ->
            val context = execution as PlatformExecutionContext; val revision = context.state.snapshot.entries.getValue(context.definition.key)
            val input = context.invocation().derive().target(SkillActor.of(f.legacy.caster)).build()
            context.services().require(AuraManager::class.java).apply(AuraDefinition(SkillKey("arcana", "buff"), revision), input, AuraOptions(100, stackLimit = 2, shareCaster = sharing))
                .thenApply { handles.add(it); SkillResult.success() }
        } }
        f.write(effects = "  - type: apply_aura"); f.load(); f.cast().join(); f.cast(input = f.invocation(mob = true)).join()
        assertNotEquals(handles[0].id(), handles[1].id()); assertEquals(2, f.platform.auras.size)
        f.engine.cancel(f.legacy.playerId); assertEquals(0, f.platform.auras.size)
        sharing = true; f.cast().join(); f.cast(input = f.invocation(mob = true)).join()
        assertSame(handles[2], handles[3]); assertEquals(2, (handles.last() as AuraInstance).stacks)
        f.engine.cancel(f.invocation(mob = true).actor().id()); assertTrue(handles.last().active()) // first caster retains ownership
        f.engine.cancel(f.legacy.playerId); assertEquals(0, f.platform.managed.size)
    }

    @Test fun concurrentDispelAndRegistrationRemovalCleanOnceAndCannotRestartEndWork() {
        val f = PlatformFixture(directory); lateinit var aura: AuraInstance
        val cleanup = java.util.concurrent.atomic.AtomicInteger(); val ends = java.util.concurrent.atomic.AtomicInteger()
        val factory = f.register("apply_aura") { _, _ -> CompiledSkillMechanic { execution ->
            val context = execution as PlatformExecutionContext; val revision = context.state.snapshot.entries.getValue(context.definition.key)
            val definition = AuraDefinition(SkillKey("arcana", "buff"), revision, onEnd = listOf(CompiledSkillMechanic { ends.incrementAndGet(); f.success() }))
            f.platform.auras.apply(definition, context.invocation(), AuraOptions(100), context).thenApply { value -> aura = value as AuraInstance; aura.managed.own(AutoCloseable { cleanup.incrementAndGet() }); SkillResult.success() }
        } }
        f.write(effects = "  - type: apply_aura"); f.load(); f.cast().join()
        val pool = java.util.concurrent.Executors.newFixedThreadPool(4)
        try {
            val tasks = (1..32).map { index -> CompletableFuture.runAsync({ if (index == 16) factory.close() else aura.remove(SkillRemovalReason.DISPELLED) }, pool) }
            CompletableFuture.allOf(*tasks.toTypedArray()).join()
            assertEquals(1, cleanup.get()); assertTrue(ends.get() in 0..1); assertEquals(0, f.platform.managed.size)
            val completed = ends.get(); f.scheduler.advance(100); assertEquals(completed, ends.get())
        } finally { pool.shutdownNow() }
    }
    @Test fun exhaustedCallbackBudgetAndRetiredTargetFailClosedWithNativeCleanup() {
        val f = PlatformFixture(directory); lateinit var aura: AuraInstance; var ticks = 0; var cleanups = 0
        f.register("apply_aura") { _, _ -> CompiledSkillMechanic { execution ->
            val context = execution as PlatformExecutionContext; val revision = context.state.snapshot.entries.getValue(context.definition.key)
            val definition = AuraDefinition(SkillKey("arcana", "buff"), revision, onTick = listOf(CompiledSkillMechanic { ticks++; f.success() }))
            f.platform.auras.apply(definition, context.invocation(), AuraOptions(100, 1), context).thenApply { aura = it as AuraInstance; aura.managed.own(AutoCloseable { cleanups++ }); SkillResult.success() }
        } }
        f.write(effects = "  - type: apply_aura"); f.load(); f.cast().join()
        repeat(4095) { aura.managed.callback() }; f.scheduler.advance(1)
        assertEquals(SkillRemovalReason.BUDGET_EXCEEDED, aura.reason().orElseThrow()); assertEquals(0, ticks); assertEquals(1, cleanups)
        f.cast().join(); io.mockk.every { f.legacy.caster.isOnline } answers { f.scheduler.assertOwner(f.legacy.caster); false }
        f.scheduler.advance(1); assertEquals(SkillRemovalReason.ACTOR_RETIRED, aura.reason().orElseThrow()); assertEquals(2, cleanups); assertEquals(0, f.platform.managed.size)
    }

    @Test fun cancelledRefreshCannotModifyAnExistingAuraAfterItsCallerStops() {
        val f = PlatformFixture(directory); lateinit var aura: AuraInstance; var pause = false
        f.register("apply_aura") { _, _ -> CompiledSkillMechanic { execution ->
            val context = execution as PlatformExecutionContext; val revision = context.state.snapshot.entries.getValue(context.definition.key)
            if (pause) f.scheduler.immediate = false
            f.platform.auras.apply(AuraDefinition(SkillKey("arcana", "buff"), revision), context.invocation(), AuraOptions(100, stackLimit = 2), context).thenApply { aura = it as AuraInstance; SkillResult.success() }
        } }
        f.write(effects = "  - type: apply_aura"); f.load(); f.cast().join(); assertEquals(1, aura.stacks)
        pause = true; val refresh = f.cast(); assertFalse(refresh.isDone)
        refresh.cancel(false); f.scheduler.advance(0)
        assertTrue(aura.active()); assertEquals(1, aura.stacks)
        f.engine.cancel(f.legacy.playerId)
    }

}
