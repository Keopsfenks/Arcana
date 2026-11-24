package com.keopsfenks.arcana.skills.components

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.providers.*
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class VanillaResourceTests {
    @TempDir lateinit var directory: Path
    @Test fun localHoldsRequireOwnershipAreIdempotentAndCannotKillTheirActor() {
        val f = NativeComponentFixture(directory)
        var health = 20.0
        every { f.root.caster.health } answers { f.scheduler.assertOwner(f.root.caster); health }
        every { f.root.caster.health = any() } answers { f.scheduler.assertOwner(f.root.caster); health = firstArg() }
        val provider = HealthResourceProvider(f.scheduler)
        val actor = SkillActor.of(f.root.caster)
        assertEquals(SkillResult.Status.UNSUPPORTED_CONTEXT, provider.reserveLocal(actor, 1.0).result().status())
        f.scheduler.on(f.root.caster) {
            assertEquals(SkillResult.Status.INSUFFICIENT_RESOURCE, provider.reserveLocal(actor, 20.0).result().status())
            assertEquals(20.0, health)
            val returned = provider.reserveLocal(actor, 5.0)
            assertEquals(15.0, health); returned.release(); returned.release(); assertEquals(20.0, health)
            val committed = provider.reserveLocal(actor, 5.0)
            committed.commit(); committed.commit(); committed.release()
            assertEquals(15.0, health)
        }.future.join()
        assertEquals(SkillResult.Status.UNSUPPORTED_ACTOR, FoodResourceProvider(f.scheduler).reserveLocal(SkillActor.of(f.root.victim), 1.0).result().status())
    }
    @Test fun nativeCostFailureAndCooldownDenialRefundHealthFoodAndOnlyTheirSaturationDelta() {
        val f = NativeComponentFixture(directory)
        var health = 20.0; var food = 3; var saturation = 2f
        every { f.root.caster.health } answers { f.scheduler.assertOwner(f.root.caster); health }
        every { f.root.caster.health = any() } answers { f.scheduler.assertOwner(f.root.caster); health = firstArg() }
        every { f.root.caster.foodLevel } answers { f.scheduler.assertOwner(f.root.caster); food }
        every { f.root.caster.foodLevel = any() } answers { f.scheduler.assertOwner(f.root.caster); food = firstArg() }
        every { f.root.caster.saturation } answers { f.scheduler.assertOwner(f.root.caster); saturation }
        every { f.root.caster.saturation = any() } answers { f.scheduler.assertOwner(f.root.caster); saturation = firstArg() }
        for (provider in listOf(HealthResourceProvider(f.scheduler), FoodResourceProvider(f.scheduler))) f.platform.register(f.root.plugin, SkillComponentType.RESOURCE, SkillKey("arcana", provider.descriptor().id()), provider)
        f.write(options = "cooldown: 1s\ncosts:\n  - {resource: health, amount: 5}\n  - {resource: food, amount: 5}", effects = "  - type: mark")
        f.load()
        assertEquals(SkillResult.Status.INSUFFICIENT_RESOURCE, f.cast().join().status())
        assertEquals(20.0, health); assertEquals(3, food); assertTrue(f.marks.isEmpty())
        food = 10
        assertTrue(f.cast().join().isSuccess)
        assertEquals(15.0, health); assertEquals(5, food)
        assertEquals(SkillResult.Status.COOLDOWN, f.cast().join().status())
        assertEquals(15.0, health); assertEquals(5, food); assertEquals(2f, saturation)
    }
    @Test fun vanillaExperienceBoundariesAndLargePointBalancesRoundTripWithoutUsingLifetimeExperience() {
        assertEquals(352, VanillaExperience.pointsForLevel(16))
        assertEquals(394, VanillaExperience.pointsForLevel(17))
        assertEquals(1507, VanillaExperience.pointsForLevel(31))
        assertEquals(1628, VanillaExperience.pointsForLevel(32))
        val points = listOf(0, 1, 6, 7, 8, 351, 352, 353, 393, 394, 1506, 1507, 1508, 1627, 1628, 1629, 100_000, 1_000_000, Int.MAX_VALUE)
        for (value in points) {
            val progress = VanillaExperience.split(value)
            assertEquals(value, VanillaExperience.total(progress.level, progress.progress), "Point balance $value")
        }
        val f = NativeComponentFixture(directory)
        var level = 17; var progress = (24.0 / 47).toFloat()
        every { f.root.caster.level } answers { f.scheduler.assertOwner(f.root.caster); level }
        every { f.root.caster.level = any() } answers { f.scheduler.assertOwner(f.root.caster); level = firstArg() }
        every { f.root.caster.exp } answers { f.scheduler.assertOwner(f.root.caster); progress }
        every { f.root.caster.exp = any() } answers { f.scheduler.assertOwner(f.root.caster); progress = firstArg() }
        val provider = ExperienceResourceProvider(f.scheduler)
        f.scheduler.on(f.root.caster) {
            val actor = SkillActor.of(f.root.caster)
            assertEquals(418.0, provider.balanceLocal(actor))
            val hold = provider.reserveLocal(actor, 18.0)
            assertEquals(400.0, provider.balanceLocal(actor))
            hold.release(); hold.release(); assertEquals(418.0, provider.balanceLocal(actor))
            assertThrows(IllegalArgumentException::class.java) { provider.validateAmount(.5) }
        }.future.join()
        verify(exactly = 0) { f.root.caster.totalExperience }
    }
}
