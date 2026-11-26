package com.keopsfenks.arcana.skills.platform.holders
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

class HolderLifecycleTests {
    @TempDir lateinit var directory: Path
    private fun owner(name: String) = mockk<Plugin>().also { every { it.name } returns name }
    private fun setup(): Triple<PlatformFixture, MutableList<SkillKey>, MutableList<SkillKey>> {
        val f = PlatformFixture(directory); val applied = mutableListOf<SkillKey>(); val closed = mutableListOf<SkillKey>()
        f.register("native_mark") { _, _ -> CompiledSkillMechanic { f.success() } }
        f.platform.register(f.legacy.plugin, SkillComponentType.CONTRIBUTION, SkillKey("arcana", "stat"), object : SkillContribution(SkillDescriptor.of("stat", "test")) {
            override fun compile(arguments: SkillArguments, compilation: SkillCompilation) = CompiledSkillContribution { _, id ->
                applied.add(id); CompletableFuture.completedFuture(object : SkillContributionLease {
                    override fun stats() = mapOf(SkillKey("arcana", "power") to 5.0)
                    override fun close() { closed.add(id) }
                })
            }
        })
        f.write(options = "activation: holder\npassives: [{type: stat}]"); f.load()
        return Triple(f, applied, closed)
    }
    private fun provider(f: PlatformFixture, owner: Plugin, name: String, callback: () -> CompletableFuture<List<SkillHolder>>) = f.platform.register(owner, SkillComponentType.HOLDER, SkillKey(name, "slots"), object : SkillHolderProvider {
        override fun descriptor() = SkillDescriptor.of("slots", "test")
        override fun holders(actor: SkillActor) = callback()
    })
    private fun holder(owner: String = "alpha", slot: String = "main") = SkillHolder(SkillKey(owner, "ring"), slot, listOf(SkillKey("arcana", "native")), emptyMap())
    @Test fun slotDiffAndOwnerRemovalOnlyCloseTheirOwnContributionsOnce() {
        val (f, applied, closed) = setup(); val alpha = owner("Alpha"); val beta = owner("Beta"); var slot = "main"
        provider(f, alpha, "alpha") { CompletableFuture.completedFuture(listOf(holder(slot = slot))) }
        provider(f, beta, "beta") { CompletableFuture.completedFuture(listOf(holder("beta"))) }
        val actor = f.invocation().actor()
        assertEquals(2, f.platform.refreshHolders(actor).toCompletableFuture().join().size)
        assertEquals(2, applied.size); assertEquals(10.0, f.platform.contributions.snapshot(actor)[SkillKey("arcana", "power")])
        f.platform.refreshHolders(actor).toCompletableFuture().join(); assertEquals(2, applied.size)
        slot = "off"; f.platform.refreshHolders(actor).toCompletableFuture().join()
        assertEquals(3, applied.size); assertEquals(1, closed.size)
        f.engine.closeOwner(alpha); assertEquals(2, closed.size); assertEquals(5.0, f.platform.contributions.snapshot(actor)[SkillKey("arcana", "power")])
        f.engine.cancel(actor.id()); assertEquals(3, closed.size); assertTrue(f.platform.contributions.snapshot(actor).isEmpty())
        f.engine.shutdown(); assertEquals(3, closed.size)
    }
    @Test fun refreshCoalescesAndLateProviderOrLeaseCompletionCannotResurrectAfterLogout() {
        val (f, applied, closed) = setup(); val alpha = owner("Alpha"); var calls = 0
        val pending = CompletableFuture<List<SkillHolder>>()
        provider(f, alpha, "alpha") { calls++; pending }
        val actor = f.invocation().actor()
        val first = f.platform.refreshHolders(actor).toCompletableFuture()
        val second = f.platform.refreshHolders(actor).toCompletableFuture()
        assertSame(first, second); f.scheduler.advance(100); assertEquals(1, calls)
        f.engine.cancel(actor.id()); pending.complete(listOf(holder()))
        assertTrue(first.isDone); assertTrue(applied.isEmpty()); assertTrue(closed.isEmpty()); assertTrue(f.platform.holders(actor).isEmpty())
    }
    @Test fun fallbackVisitsOnlyTrackedActorsAtTwentyTickIntervalsAndProviderDisappearanceClearsLeases() {
        val (f, applied, closed) = setup(); val alpha = owner("Alpha"); var calls = 0
        val handle = provider(f, alpha, "alpha") { calls++; CompletableFuture.completedFuture(listOf(holder())) }
        val actor = f.invocation().actor(); f.platform.refreshHolders(actor).toCompletableFuture().join()
        f.scheduler.advance(19); assertEquals(1, calls)
        f.scheduler.advance(1); assertEquals(2, calls); assertEquals(1, applied.size)
        handle.close(); assertEquals(1, closed.size); assertTrue(f.platform.holders(actor).isEmpty())
        f.scheduler.advance(20); assertEquals(2, calls)
        f.engine.shutdown()
    }
    @Test fun holderOnlyTriggerUsesActiveHolderParametersAndStopsWhenTheHolderIsRemoved() {
        val (f, _, _) = setup(); val alpha = owner("Alpha"); val marks = mutableListOf<Double>()
        f.register("parameter_mark") { _, _ -> CompiledSkillMechanic { execution -> marks.add(execution.invocation().parameters().getValue("power").number()); f.success() } }
        f.write(options = "activation: holder\ntriggers: [attack]\nparameters:\n  power: {type: number, default: 2}", effects = "  - type: parameter_mark"); f.load()
        val actor = f.invocation().actor()
        assertTrue(f.platform.trigger(SkillKey("arcana", "attack"), f.invocation()).toCompletableFuture().join().isEmpty())
        val handle = provider(f, alpha, "alpha") { CompletableFuture.completedFuture(listOf(SkillHolder(SkillKey("alpha", "ring"), "main", listOf(SkillKey("arcana", "native")), mapOf("power" to SkillValue.of(9))))) }
        f.platform.refreshHolders(actor).join()
        val results = f.platform.trigger(SkillKey("arcana", "attack"), f.invocation()).toCompletableFuture().join()
        assertEquals(1, results.size); assertTrue(results.single().isSuccess); assertEquals(listOf(9.0), marks)
        handle.close(); assertTrue(f.platform.trigger(SkillKey("arcana", "attack"), f.invocation()).toCompletableFuture().join().isEmpty())
    }

    @Test fun heldInterceptionIsInactiveWithoutItsHolderAndUsesItsParametersWhenActive() {
        val (f, _, _) = setup(); val alpha = owner("Alpha")
        f.platform.register(f.legacy.plugin, SkillComponentType.INTERCEPTOR, SkillKey("arcana", "power"), object : SkillInterceptor(SkillDescriptor.of("power", "test")) {
            override fun compile(arguments: SkillArguments, compilation: SkillCompilation) = CompiledSkillInterceptor { event, invocation, _ -> event.damage(event.damage() + invocation.parameters().getValue("power").number()); SkillResult.success() }
        })
        f.write(options = "activation: holder\ntriggers: [attack]\nparameters:\n  power: {type: number, default: 2}\ninterceptors: [{type: power}]"); f.load()
        var damage = 4.0
        val event = mockk<org.bukkit.event.entity.EntityDamageEvent>()
        every { event.damage } answers { damage }; every { event.setDamage(any<Double>()) } answers { damage = firstArg() }; every { event.isCancelled } returns false
        val listener = f.platform.interceptorListener
        f.scheduler.on(f.legacy.caster) { listener.intercept(SkillKey("arcana", "attack"), f.invocation(), event) }.future.join()
        assertEquals(4.0, damage); assertEquals(0, listener.pendingCount)
        provider(f, alpha, "alpha") { CompletableFuture.completedFuture(listOf(SkillHolder(SkillKey("alpha", "ring"), "main", listOf(SkillKey("arcana", "native")), mapOf("power" to SkillValue.of(9))))) }
        f.platform.refreshHolders(f.invocation().actor()).join()
        f.scheduler.on(f.legacy.caster) { listener.intercept(SkillKey("arcana", "attack"), f.invocation(), event) }.future.join()
        assertEquals(13.0, damage); assertEquals(1, listener.pendingCount)
        listener.close(); assertEquals(0, listener.pendingCount)
    }

    @Test fun hungProviderRefreshExpiresAndCannotKeepAnOldPassiveAliveIndefinitely() {
        val (f, applied, closed) = setup(); val alpha = owner("Alpha"); var wait = false
        val pending = CompletableFuture<List<SkillHolder>>()
        provider(f, alpha, "alpha") { if (wait) pending else CompletableFuture.completedFuture(listOf(holder())) }
        val actor = f.invocation().actor(); f.platform.refreshHolders(actor).join(); assertEquals(1, applied.size)
        wait = true; val refresh = f.platform.refreshHolders(actor)
        f.scheduler.advance(99); assertFalse(refresh.isDone); assertTrue(closed.isEmpty())
        f.scheduler.advance(1); assertTrue(refresh.isCompletedExceptionally); assertEquals(1, closed.size); assertTrue(f.platform.holders(actor).isEmpty())
        pending.complete(listOf(holder())); assertEquals(1, applied.size); assertEquals(0, f.platform.managed.size)
    }

}
