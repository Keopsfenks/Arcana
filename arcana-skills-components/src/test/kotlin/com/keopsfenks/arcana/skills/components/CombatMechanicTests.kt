package com.keopsfenks.arcana.skills.components
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.effects.combat.*
import com.keopsfenks.arcana.skills.components.effects.status.*
import com.keopsfenks.arcana.skills.execution.SkillDamageScope
import io.mockk.*
import org.bukkit.NamespacedKey
import org.bukkit.attribute.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class CombatMechanicTests {
    @TempDir lateinit var directory: Path
    private fun fixture() = NativeComponentFixture(directory).also { it.install(listOf(DamageMechanic(), HealMechanic(), FeedMechanic(), IgniteMechanic(), ExtinguishMechanic(), AbsorptionMechanic(), FreezeMechanic())) }
    @Test fun actualDamageIsGuardedOnVictimOwnerAndUsesNormalServerDamage() {
        val f = fixture(); var amount = 0.0
        every { f.root.victim.damage(any<Double>()) } answers { f.scheduler.assertOwner(f.root.victim); assertTrue(SkillDamageScope.isActive()); amount = firstArg(); Unit }
        f.write(effects = "  - {type: damage, amount: 4}"); f.load()
        assertTrue(f.platform.cast(SkillKey("arcana", "one"), f.invocation().derive().target(SkillActor.of(f.root.victim)).build()).toCompletableFuture().join().isSuccess)
        assertEquals(4.0, amount); assertFalse(SkillDamageScope.isActive())
    }
    @Test fun healthAbsorptionAndFireClampOnTargetAndDeadTargetsAreSkipped() {
        val f = fixture(); var health = 18.0; var absorption = 3.0; var fire = 0
        every { f.root.victim.health } answers { f.scheduler.assertOwner(f.root.victim); health }
        every { f.root.victim.health = any() } answers { f.scheduler.assertOwner(f.root.victim); health = firstArg(); Unit }
        every { f.root.victim.absorptionAmount } answers { f.scheduler.assertOwner(f.root.victim); absorption }
        every { f.root.victim.absorptionAmount = any() } answers { f.scheduler.assertOwner(f.root.victim); absorption = firstArg(); Unit }
        every { f.root.victim.fireTicks } answers { f.scheduler.assertOwner(f.root.victim); fire }
        every { f.root.victim.fireTicks = any() } answers { f.scheduler.assertOwner(f.root.victim); fire = firstArg(); Unit }
        f.write(effects = "  - {type: heal, amount: 10}\n  - {type: absorption, amount: 10, maximum: 8}\n  - {type: ignite, duration: 2s}")
        f.write("clear", effects = "  - {type: extinguish}"); f.load()
        val input = f.invocation().derive().target(SkillActor.of(f.root.victim)).build()
        assertTrue(f.platform.cast(SkillKey("arcana", "one"), input).toCompletableFuture().join().isSuccess)
        assertEquals(20.0, health); assertEquals(8.0, absorption); assertEquals(40, fire)
        f.platform.cast(SkillKey("arcana", "clear"), input).toCompletableFuture().join(); assertEquals(0, fire)
        every { f.root.victim.isDead } answers { f.scheduler.assertOwner(f.root.victim); true }
        assertEquals(SkillResult.Status.NO_TARGET, f.platform.cast(SkillKey("arcana", "one"), input).toCompletableFuture().join().status())
    }
    @Test fun explicitMissingTargetAndPlayerOnlyFeedReturnCapabilityResults() {
        val f = fixture(); f.write(effects = "  - {type: damage, target: target, amount: 1}"); f.write("food", effects = "  - {type: feed, amount: 2}"); f.load()
        assertEquals(SkillResult.Status.NO_TARGET, f.cast().join().status()); assertEquals(SkillResult.Status.UNSUPPORTED_ACTOR, f.cast("food", true).join().status())
        var food = 19; var saturation = 19f
        every { f.root.caster.foodLevel } answers { f.scheduler.assertOwner(f.root.caster); food }
        every { f.root.caster.foodLevel = any() } answers { f.scheduler.assertOwner(f.root.caster); food = firstArg(); Unit }
        every { f.root.caster.saturation } answers { f.scheduler.assertOwner(f.root.caster); saturation }
        every { f.root.caster.saturation = any() } answers { f.scheduler.assertOwner(f.root.caster); saturation = firstArg(); Unit }
        assertTrue(f.cast("food").join().isSuccess); assertEquals(20, food); assertTrue(saturation <= food)
    }
    @Test fun stunRemovesOnlyItsOwnTransientMovementContributions() {
        val f = fixture(); val attribute = mockk<Attribute>(); val instance = mockk<AttributeInstance>(); val modifiers = mutableListOf<AttributeModifier>()
        every { attribute.key } returns NamespacedKey.minecraft("movement_speed")
        every { f.root.caster.getAttribute(attribute) } answers { f.scheduler.assertOwner(f.root.caster); instance }
        every { instance.modifiers } answers { f.scheduler.assertOwner(f.root.caster); modifiers.toList() }
        every { instance.addTransientModifier(any()) } answers { f.scheduler.assertOwner(f.root.caster); modifiers.add(firstArg()); Unit }
        every { instance.removeModifier(any<AttributeModifier>()) } answers { f.scheduler.assertOwner(f.root.caster); modifiers.remove(firstArg<AttributeModifier>()); Unit }
        val external = AttributeModifier(NamespacedKey("other", "speed"), .2, AttributeModifier.Operation.ADD_NUMBER); modifiers.add(external)
        f.install(listOf(StunMechanic { if (it == "movement_speed") attribute else null }))
        f.write(effects = "  - {type: stun, duration: 3t}"); f.write("long", effects = "  - {type: stun, duration: 10t}"); f.load()
        f.cast().join(); f.cast("long").join(); assertEquals(3, modifiers.size)
        f.scheduler.advance(3); assertEquals(2, modifiers.size); assertTrue(external in modifiers)
        f.engine.closeOwner(f.root.plugin); assertEquals(listOf(external), modifiers); assertEquals(0, f.platform.managed.size)
    }
    @Test fun freezeStopsRefreshAfterCancellationAndPreservesAnExternalFreezeLock() {
        val f = fixture(); var ticks = 0; var writes = 0; var locked = false
        every { f.root.caster.freezeTicks } answers { f.scheduler.assertOwner(f.root.caster); ticks }
        every { f.root.caster.maxFreezeTicks } answers { f.scheduler.assertOwner(f.root.caster); 140 }
        every { f.root.caster.isFreezeTickingLocked } answers { f.scheduler.assertOwner(f.root.caster); locked }
        every { f.root.caster.freezeTicks = any() } answers { f.scheduler.assertOwner(f.root.caster); ticks = firstArg(); writes++; Unit }
        f.write(effects = "  - {type: freeze, duration: 5t}"); f.load(); f.cast().join(); assertEquals(140, ticks)
        f.scheduler.advance(1); locked = true; ticks = 200; val before = writes; f.scheduler.advance(1); assertEquals(before, writes); assertEquals(200, ticks)
        f.engine.closeOwner(f.root.plugin); f.scheduler.advance(20); assertEquals(before, writes); assertEquals(0, f.platform.managed.size)
    }
}
