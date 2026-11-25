package com.keopsfenks.arcana.skills.components
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.effects.movement.*
import io.mockk.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.util.Vector
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

class MovementMechanicTests {
    @TempDir lateinit var directory: Path
    private val fixtures = mutableListOf<NativeComponentFixture>()
    @BeforeEach fun worldResolver() { mockkStatic(Bukkit::class) }
    @AfterEach fun cleanup() { fixtures.forEach { it.engine.shutdown() }; unmockkStatic(Bukkit::class) }
    private fun fixture() = NativeComponentFixture(directory).also { f -> fixtures.add(f); f.install(listOf(VelocityMechanic(), KnockbackMechanic(), PullMechanic(), LaunchMechanic(), DashMechanic(), TeleportMechanic())); every { Bukkit.getWorld(f.worldId) } answers { f.scheduler.assertOwner("global"); f.root.world } }
    @Test fun normalizedVelocityKnockbackPullAndLaunchUseOnlyOwnedMutations() {
        val f = fixture(); val velocities = mutableListOf<Vector>()
        every { f.root.victim.velocity = any() } answers { f.scheduler.assertOwner(f.root.victim); velocities.add(firstArg<Vector>().clone()); Unit }
        f.write(effects = "  - {type: velocity, x: 3, y: 0, z: 4, normalize: true, speed: 2}\n  - {type: knockback, strength: 2}\n  - {type: pull, strength: 2}\n  - {type: launch, strength: 3}"); f.load()
        val input = f.invocation().derive().target(SkillActor.of(f.root.victim)).build(); assertTrue(f.platform.cast(SkillKey("arcana", "one"), input).toCompletableFuture().join().isSuccess)
        assertEquals(Vector(1.2, 0.0, 1.6), velocities[0]); assertEquals(Vector(2.0, 0.0, 0.0), velocities[1]); assertEquals(Vector(-2.0, 0.0, 0.0), velocities[2]); assertEquals(3.0, velocities[3].y)
    }
    @Test fun zeroDistanceImpulseStaysFiniteAndDashCancelsAllFutureWrites() {
        val f = fixture(); var velocity = Vector(); var writes = 0
        every { f.root.caster.velocity } answers { f.scheduler.assertOwner(f.root.caster); velocity.clone() }
        every { f.root.caster.velocity = any() } answers { f.scheduler.assertOwner(f.root.caster); velocity = firstArg<Vector>().clone(); writes++; Unit }
        f.write(effects = "  - {type: knockback, strength: 2}\n  - {type: dash, direction: {x: 1, y: 0, z: 0}, speed: 2, duration: 5t}"); f.load(); f.cast().join()
        assertEquals(Vector(2.0, 0.0, 0.0), velocity); f.scheduler.advance(1); f.engine.closeOwner(f.root.plugin)
        assertEquals(Vector(), velocity); val stopped = writes; f.scheduler.advance(100); assertEquals(stopped, writes); assertEquals(0, f.platform.managed.size)
    }
    @Test fun teleportAwaitsNativeAsyncResultAndDoesNotUseSynchronousTeleport() {
        val f = fixture(); val pending = CompletableFuture<Boolean>()
        every { f.root.caster.teleportAsync(any<Location>()) } answers { f.scheduler.assertOwner(f.root.caster); assertEquals(20.0, firstArg<Location>().x); pending }
        f.write(effects = "  - {type: teleport, destination: {x: 20, y: 70, z: 4}}\n  - {type: mark, name: after}"); f.load()
        val result = f.cast(); assertFalse(result.isDone); assertTrue(f.marks.isEmpty()); pending.complete(true)
        assertTrue(result.join().isSuccess); assertEquals(listOf("after"), f.marks)
        verify(exactly = 0) { f.root.caster.teleport(any<Location>()) }
    }
    @Test fun failedAndCancelledTeleportsCannotExecuteFollowingEffects() {
        val f = fixture(); var pending = CompletableFuture.completedFuture(false)
        every { f.root.caster.teleportAsync(any<Location>()) } answers { f.scheduler.assertOwner(f.root.caster); pending }
        f.write(effects = "  - {type: teleport, destination: {x: 20, y: 70, z: 4}}\n  - {type: mark, name: after}"); f.load()
        assertEquals(SkillResult.Status.NO_TARGET, f.cast().join().status()); assertTrue(f.marks.isEmpty())
        pending = CompletableFuture(); val cancelled = f.cast(); f.engine.closeOwner(f.root.plugin); pending.complete(true)
        assertFalse(cancelled.join().isSuccess); assertTrue(f.marks.isEmpty())
    }
    @Test fun explicitTriggerDashRetiresWithItsRecipientAndKeepsALaterExternalImpulse() {
        val f = fixture(); var velocity = Vector(); var writes = 0
        every { f.root.victim.velocity } answers { f.scheduler.assertOwner(f.root.victim); velocity.clone() }
        every { f.root.victim.velocity = any() } answers { f.scheduler.assertOwner(f.root.victim); velocity = firstArg<Vector>().clone(); writes++; Unit }
        f.write(effects = "  - {type: dash, target: trigger, direction: {x: 1, y: 0, z: 0}, speed: 2, duration: 100t}"); f.load()
        val recipient = SkillActor.of(f.root.victim); val input = f.invocation().derive().trigger(recipient).build()
        assertTrue(f.platform.cast(SkillKey("arcana", "one"), input).toCompletableFuture().join().isSuccess)
        velocity = Vector(0.0, 2.0, 0.0); val before = writes
        f.engine.cancel(recipient.id()); assertEquals(0, f.platform.managed.size); assertEquals(Vector(0.0, 2.0, 0.0), velocity)
        f.scheduler.advance(100); assertEquals(before, writes)
    }
}
