package com.keopsfenks.arcana.skills.platform.events

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.execution.PlatformFixture
import io.mockk.*
import org.bukkit.event.entity.EntityDamageEvent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

class InterceptorAdmissionTests {
    @TempDir lateinit var directory: Path
    private fun fixture() = PlatformFixture(directory).also { f ->
        f.register("native_mark") { _, _ -> CompiledSkillMechanic { f.legacy.marks.add("mark"); f.success() } }
    }
    private fun interceptor(f: PlatformFixture, name: String = "add", action: (SkillEventAccess) -> Unit = { it.damage(it.damage() + 3) }) =
        f.platform.register(f.legacy.plugin, SkillComponentType.INTERCEPTOR, SkillKey("arcana", name), object : SkillInterceptor(SkillDescriptor.of(name, "test")) {
            override fun compile(arguments: SkillArguments, compilation: SkillCompilation) = CompiledSkillInterceptor { event, _, _ -> action(event); SkillResult.success() }
        })
    private fun event(): EntityDamageEvent {
        var damage = 4.0; var cancelled = false
        return mockk<EntityDamageEvent>().also { e ->
            every { e.finalDamage } answers { damage }; every { e.damage } answers { damage }; every { e.setDamage(any<Double>()) } answers { damage = firstArg() }
            every { e.isCancelled } answers { cancelled }; every { e.isCancelled = any() } answers { cancelled = firstArg() }
        }
    }
    @Test fun synchronousMutationScopedFacadeAndSingleUseContinuationShareOneAdmission() {
        val f = fixture(); var retained: SkillEventAccess? = null; var calls = 0
        interceptor(f) { retained = it; it.damage(it.damage() + 3) }
        f.platform.register(f.legacy.plugin, SkillComponentType.RESOURCE, SkillKey("arcana", "local"), object : SkillResourceProvider {
            override fun descriptor() = SkillDescriptor.of("local", "test")
            override fun supportsLocal() = true
            override fun reserve(actor: SkillActor, amount: Double) = error("async reserve called")
            override fun reserveLocal(actor: SkillActor, amount: Double): SkillResourceReservation { calls++; return object : SkillResourceReservation { override fun commit() {} ; override fun release() {} } }
        })
        f.write(options = "interceptors: [{type: add}]\ncooldown: 1s\ncosts: [{resource: local}]"); f.load()
        val event = event()
        val outcome = f.scheduler.on(f.legacy.caster) { f.platform.interceptors.intercept(SkillKey("arcana", "native"), f.invocation(), event) }.future.join()
        assertTrue(outcome.result.isSuccess, outcome.result.message()); assertEquals(7.0, event.damage)
        assertThrows(IllegalStateException::class.java) { retained!!.damage() }
        val eventField = retained!!.javaClass.getDeclaredField("event").also { it.isAccessible = true }
        assertNull(eventField.get(retained))
        assertEquals(1, calls); assertTrue(f.legacy.marks.isEmpty())
        assertEquals(SkillResult.Status.COOLDOWN, f.cast().join().status())
        assertTrue(outcome.continuation!!.resume().join().isSuccess)
        assertEquals(listOf("mark"), f.legacy.marks); assertEquals(2, calls) // competing attempt prepared then released
        assertEquals(SkillResult.Status.CANCELLED, outcome.continuation!!.resume().join().status())
    }
    @Test fun cancelledOrForeignOwnedEventsPerformNoReadsAndReloadInvalidatesContinuation() {
        val f = fixture(); interceptor(f); f.write(options = "interceptors: [{type: add}]"); f.load()
        val foreign = mockk<EntityDamageEvent>()
        assertEquals(SkillResult.Status.UNSUPPORTED_CONTEXT, f.platform.interceptors.intercept(SkillKey("arcana", "native"), f.invocation(), foreign).result.status())
        verify { foreign wasNot Called }
        val event = event(); event.isCancelled = true
        val cancelled = f.scheduler.on(f.legacy.caster) { f.platform.interceptors.intercept(SkillKey("arcana", "native"), f.invocation(), event) }.future.join()
        assertEquals(SkillResult.Status.CANCELLED, cancelled.result.status())
        event.isCancelled = false
        val accepted = f.scheduler.on(f.legacy.caster) { f.platform.interceptors.intercept(SkillKey("arcana", "native"), f.invocation(), event) }.future.join()
        f.load(); assertEquals(SkillResult.Status.CANCELLED, accepted.continuation!!.resume().join().status()); assertTrue(f.legacy.marks.isEmpty())
    }
    @Test fun interceptorCompilationRejectsAsyncCostDelayAndSixtyFifthLocalStep() {
        val f = fixture(); interceptor(f)
        f.write(options = "interceptors: [{type: add, delay: 1s}]"); assertFalse(f.engine.reload().success())
        f.write(options = "interceptors: [{type: projectile}]"); assertFalse(f.engine.reload().success())
        f.write(options = "interceptors:\n" + (1..65).joinToString("\n") { "  - {type: add}" }); assertFalse(f.engine.reload().success())
        f.platform.register(f.legacy.plugin, SkillComponentType.RESOURCE, SkillKey("arcana", "async"), object : SkillResourceProvider {
            override fun descriptor() = SkillDescriptor.of("async", "test")
            override fun reserve(actor: SkillActor, amount: Double) = CompletableFuture.completedFuture(SkillResourceReservation.denied(SkillResult.of(SkillResult.Status.ERROR, "unused")))
        })
        f.write(options = "interceptors: [{type: add}]\ncosts: [{resource: async}]"); assertFalse(f.engine.reload().success())
    }
    @Test fun highestAndMonitorHandoffKeepsNoEventAndDropsLaterCancelledContinuations() {
        val f = fixture(); interceptor(f)
        f.write(options = "triggers: [attack]\ninterceptors: [{type: add}]"); f.load()
        val listener = InterceptorListener(f.engine, f.platform)
        val event = event()
        f.scheduler.on(f.legacy.caster) { listener.intercept(SkillKey("arcana", "attack"), f.invocation(), event) }.future.join()
        assertEquals(7.0, event.damage); assertEquals(1, listener.pendingCount)
        event.isCancelled = true
        listener.observe(event)
        assertEquals(0, listener.pendingCount); f.scheduler.advance(1); assertTrue(f.legacy.marks.isEmpty())
        event.isCancelled = false
        f.scheduler.on(f.legacy.caster) { listener.intercept(SkillKey("arcana", "attack"), f.invocation(), event) }.future.join()
        listener.observe(event); f.scheduler.advance(1)
        assertEquals(listOf("mark"), f.legacy.marks); assertEquals(0, listener.pendingCount)
        val highest = InterceptorListener::class.java.getMethod("onHighest", EntityDamageEvent::class.java).getAnnotation(org.bukkit.event.EventHandler::class.java)
        val monitor = InterceptorListener::class.java.getMethod("onMonitor", EntityDamageEvent::class.java).getAnnotation(org.bukkit.event.EventHandler::class.java)
        assertEquals(org.bukkit.event.EventPriority.HIGHEST, highest.priority); assertTrue(highest.ignoreCancelled)
        assertEquals(org.bukkit.event.EventPriority.MONITOR, monitor.priority); assertFalse(monitor.ignoreCancelled)
        listener.close()
    }

    @Test fun nonLocalPredicatesAndPersistentBindingsAreRejectedBeforePublication() {
        val f = fixture(); interceptor(f)
        f.platform.register(f.legacy.plugin, SkillComponentType.PREDICATE, SkillKey("arcana", "external"), object : SkillPredicateType(SkillDescriptor.of("external", "test")) {
            override fun compile(arguments: SkillArguments, compilation: SkillCompilation) = SkillPredicate { true }
        })
        f.write(options = "interceptors: [{type: add}]\nconditions: [{type: external}]")
        assertFalse(f.engine.reload().success())
        f.write(options = "interceptors: [{type: add}]\nbindings:\n  var_combo: {scope: actor, key: combo, type: number, persistent: true}")
        assertFalse(f.engine.reload().success())
    }

}
