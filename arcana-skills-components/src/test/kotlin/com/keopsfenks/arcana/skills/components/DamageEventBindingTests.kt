package com.keopsfenks.arcana.skills.components
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.conditions.*
import com.keopsfenks.arcana.skills.components.interceptors.CancelEventInterceptor
import com.keopsfenks.arcana.skills.components.triggers.EntityDamagedSkillTrigger
import io.mockk.*
import org.bukkit.event.entity.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class DamageEventBindingTests {
    private fun fixture(dir: Path) = NativeComponentFixture(dir).also { f ->
        f.platform.register(f.root.plugin, SkillComponentType.TRIGGER, SkillKey("arcana", "entity_damaged"), EntityDamagedSkillTrigger(f.root.plugin))
        f.platform.register(f.root.plugin, SkillComponentType.INTERCEPTOR, SkillKey("arcana", "cancel_event"), CancelEventInterceptor())
        for (type in listOf(EntityTypePredicate(), VariablePredicate())) f.platform.register(f.root.plugin, SkillComponentType.PREDICATE, SkillKey("arcana", type.descriptor().id()), type)
    }
    private fun event(f: NativeComponentFixture): EntityDamageByEntityEvent {
        val event = mockk<EntityDamageByEntityEvent>(); var cancelled = false
        every { event.isCancelled } answers { cancelled }; every { event.setCancelled(any()) } answers { cancelled = firstArg() }
        every { event.entity } returns f.root.victim; every { event.damager } returns f.root.caster
        every { event.damage } returns 2.0; every { event.finalDamage } returns 1.0; every { event.cause } returns EntityDamageEvent.DamageCause.ENTITY_ATTACK
        return event
    }
    @Test fun locallyOwnedCounterpartAndCauseMatchDuringTheSynchronousPhase(@TempDir dir: Path) {
        val f = fixture(dir); f.scheduler.regionPeers.addAll(listOf(f.root.caster, f.root.victim))
        f.write(options = "triggers: [entity_damaged]\ninterceptors: [{type: cancel_event}]\nconditions:\n  - {type: entity_type, subject: target, types: [PLAYER]}\n  - {type: entity_type, subject: trigger, types: [PLAYER]}\n  - {type: variable, binding: data.cause, equals: ENTITY_ATTACK}", effects = "  - type: mark")
        f.load(); val event = event(f)
        f.scheduler.on(f.root.victim) { f.platform.interceptorListener.onHighest(event); f.platform.interceptorListener.onMonitor(event) }.future.join()
        assertTrue(event.isCancelled, "Synchronous damage input lost counterpart/cause"); assertTrue(f.marks.isEmpty()); f.engine.shutdown()
    }
    @Test fun foreignCounterpartIsNotReadLocallyButIsCapturedForNormalContinuation(@TempDir dir: Path) {
        val f = fixture(dir)
        f.platform.register(f.root.plugin, SkillComponentType.INTERCEPTOR, SkillKey("arcana", "noop"), object : SkillInterceptor(SkillDescriptor.of("noop", "test")) {
            override fun compile(arguments: SkillArguments, compilation: SkillCompilation) = CompiledSkillInterceptor { _, input, _ -> assertTrue(input.trigger().isEmpty); SkillResult.success() }
        })
        f.register("roles") { _, _ -> CompiledSkillMechanic { context ->
            val input = context.invocation(); assertEquals(f.root.caster.uniqueId, input.target().orElse(null)?.id()); assertEquals(f.root.caster.uniqueId, input.trigger().orElse(null)?.id())
            assertEquals("PLAYER", input.data()["trigger_type"]?.text()); assertEquals("ENTITY_ATTACK", input.data()["cause"]?.text()); assertEquals(1.0, input.data()["damage"]?.number())
            f.marks.add("roles"); f.success()
        } }
        f.write(options = "triggers: [entity_damaged]\ninterceptors: [{type: noop}]", effects = "  - type: roles"); f.load()
        val event = event(f); f.scheduler.on(f.root.victim) { f.platform.interceptorListener.onHighest(event); f.platform.interceptorListener.onMonitor(event) }.future.join()
        f.scheduler.advance(1); assertEquals(listOf("roles"), f.marks); f.engine.shutdown()
    }
}
