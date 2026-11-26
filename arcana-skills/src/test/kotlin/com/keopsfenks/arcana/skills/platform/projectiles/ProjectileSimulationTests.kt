package com.keopsfenks.arcana.skills.platform.projectiles

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.execution.*
import com.keopsfenks.arcana.skills.platform.managed.ManagedExecutionState
import io.mockk.every
import org.bukkit.Location
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CompletableFuture

class ProjectileSimulationTests {
    @Test fun oneTickLifetimeAppliesOneSweptStepAndThenDisposes() {
        val r = Rig(); r.contacts = { _, _ -> listOf(r.hit(1.0, .25)) }
        r.launch(ProjectileOptions(speed = 4.0, direction = SkillVector(1.0, 0.0, 0.0), lifetimeTicks = 1, pierce = 10, maxHits = 1, hitCaster = true))
        r.f.scheduler.advance(1); assertEquals(1, r.hits.size); assertFalse(r.handle.active()); assertEquals(0, r.f.platform.managed.size)
    }
    @Test fun actualSweptQueryRetiresFlightWhenMiddlePartitionDisappearsButDestinationStaysLoaded() {
        val r = Rig(realQuery = true); val calls = mutableMapOf<Int, Int>()
        every { r.f.legacy.world.isChunkLoaded(any<Int>(), any<Int>()) } answers {
            val at = r.f.scheduler.current as Location; val x = firstArg<Int>(); assertEquals(x, at.blockX shr 4)
            val n = (calls[x] ?: 0)+1; calls[x]=n; x != 1 || n == 1
        }
        every { r.f.legacy.world.minHeight } returns 0; every { r.f.legacy.world.maxHeight } returns 256
        val air = io.mockk.mockk<org.bukkit.block.Block>(); val shape = io.mockk.mockk<org.bukkit.util.VoxelShape>()
        every { shape.boundingBoxes } returns emptyList(); every { air.collisionShape } returns shape
        every { r.f.legacy.world.getBlockAt(any<Int>(), any<Int>(), any<Int>()) } answers {
            val at = r.f.scheduler.current as Location; assertEquals(firstArg<Int>() shr 4, at.blockX shr 4); air
        }
        val emptyChunk = io.mockk.mockk<org.bukkit.Chunk>(); every { emptyChunk.entities } returns emptyArray()
        every { r.f.legacy.world.getChunkAt(any<Int>(), any<Int>()) } returns emptyChunk
        every { r.f.legacy.world.getNearbyEntities(any<org.bukkit.util.BoundingBox>()) } returns emptyList()
        r.launch(ProjectileOptions(speed = 32.0, direction = SkillVector(1.0, 0.0, 0.0), lifetimeTicks = 1, radius = 0.0))
        r.f.scheduler.advance(1); assertFalse(r.handle.active()); assertEquals(r.point(0.0), r.handle.position)
        assertEquals(2, calls[1], "Middle chunk must become unavailable only on the collision pass after successful preflight")
        assertEquals(1, calls[2], "Loaded destination was reached by the complete preflight")
        assertEquals(SkillRemovalReason.CANCELLED, r.handle.reason().orElseThrow())
        assertTrue(r.hits.isEmpty()); assertEquals(0, r.f.platform.managed.size); assertEquals(1, r.cleanups)
    }
    @Test fun queuedOwnerHandoffsFinishAOneStepFlightBeforeSimulationExpiry() {
        val r = Rig(); r.launch(ProjectileOptions(speed = 4.0, direction = SkillVector(1.0, 0.0, 0.0), lifetimeTicks = 1))
        r.f.scheduler.foreignDelayTicks = 1
        repeat(12) { r.f.scheduler.advance(1) }
        assertEquals(r.point(4.0), r.handle.position); assertEquals(1, r.marks.count { it == "tick" }); assertFalse(r.handle.active())
        assertEquals(0, r.f.platform.managed.size)
    }
    @Test fun stalledQueriesRetireWithinAFiniteWallDeadlineAndDiscardLateHits() {
        val r = Rig(); val pending = CompletableFuture<List<SkillCollision>>(); r.barrier = pending
        r.launch(ProjectileOptions(lifetimeTicks = 1)); repeat(110) { r.f.scheduler.advance(1) }
        assertFalse(r.handle.active()); assertEquals(0, r.f.platform.managed.size); assertEquals(1, r.cleanups)
        pending.complete(listOf(r.hit(1.0, .25))); assertTrue(r.hits.isEmpty())
    }
    @Test fun twoBouncesUseOnlyTheRemainingFractionOfTheOriginalStep() {
        val r = Rig(); var walls = 0
        r.contacts = { from, to ->
            if (walls++ < 2) listOf(SkillCollision(r.point((from.x()+to.x())/2), SkillVector(if (to.x()>from.x()) -1.0 else 1.0, 0.0, 0.0), Optional.empty(), .5)) else emptyList()
        }
        r.launch(ProjectileOptions(speed = 8.0, direction = SkillVector(1.0, 0.0, 0.0), bounces = 2, bounceEnergy = 1.0, lifetimeTicks = 1))
        r.f.scheduler.advance(1)
        assertEquals(4.0, r.handle.position.x(), 3e-5, "8 units: +4, -2, +2 plus separation epsilon")
        assertEquals(3, r.points.size)
    }
    @TempDir lateinit var directory: Path
    private inner class Rig(realQuery: Boolean = false) {
        val f = PlatformFixture(directory)
        var loaded = true; var retired = false; var calls = 0; var cleanups = 0
        var barrier: CompletableFuture<List<SkillCollision>>? = null
        var contacts: (SkillLocation, SkillLocation) -> List<SkillCollision> = { _, _ -> emptyList() }
        val victim = SkillActor.of(f.legacy.victim)
        val points = mutableListOf<SkillLocation>(); val hits = mutableListOf<SkillActor?>(); val marks = mutableListOf<String>()
        lateinit var handle: ProjectileInstance
        var registration: SkillRegistration? = null
        var preloadCallbacks = 0
        val query = object : SkillTargetQuery {
            override fun loaded(bounds: SkillBounds) = CompletableFuture.completedFuture(loaded)
            override fun entities(bounds: SkillBounds) = CompletableFuture.completedFuture(emptyList<SkillActor>())
            override fun snapshot(actor: SkillActor) = CompletableFuture.completedFuture(if (retired) Optional.empty() else Optional.of(SkillEntitySnapshot(actor, point(4.0, 64.0, 4.0), SkillBounds(f.worldId, 3.9, 64.0, 3.9, 4.1, 65.0, 4.1), emptyMap())))
            override fun collisions(from: SkillLocation, to: SkillLocation, radius: Double): CompletableFuture<List<SkillCollision>> { calls++; points.add(to); return barrier ?: CompletableFuture.completedFuture(contacts(from, to)) }
        }
        val manager = ProjectileManager(f.engine, f.platform, queryFactory = { if (realQuery) com.keopsfenks.arcana.skills.platform.targets.RegionQuery(f.scheduler, { f.legacy.world }, state = it.state) else query }, worlds = { f.legacy.world })
        init {
            every { f.legacy.world.isChunkLoaded(any<Int>(), any<Int>()) } answers {
                val owner = f.scheduler.current as? Location ?: error("Projectile commit requires location ownership")
                assertEquals(firstArg<Int>(), owner.blockX shr 4); assertEquals(secondArg<Int>(), owner.blockZ shr 4); loaded
            }
        }
        fun point(x: Double, y: Double = 64.0, z: Double = 0.0) = SkillLocation(f.worldId, x, y, z)
        fun hit(x: Double, fraction: Double, actor: SkillActor? = victim) = SkillCollision(point(x), SkillVector(-1.0, 0.0, 0.0), Optional.ofNullable(actor), fraction)
        fun launch(options: ProjectileOptions = ProjectileOptions(speed = 4.0, direction = SkillVector(1.0, 0.0, 0.0), lifetimeTicks = 10), filter: SkillPredicate = SkillPredicate { true }, expectSuccess: Boolean = true) {
            val hooks = ProjectileHooks(
                onStart = listOf(CompiledSkillMechanic { c -> marks.add("start"); val managed = c.services().require(ManagedExecutionState::class.java); managed.own(AutoCloseable { cleanups++ }); repeat(preloadCallbacks) { managed.callback() }; f.success() }),
                onTick = listOf(CompiledSkillMechanic { marks.add("tick"); f.success() }),
                onHit = listOf(CompiledSkillMechanic { c -> hits.add(c.invocation().target().orElse(null)); assertEquals(point(0.0), c.invocation().origin().orElseThrow()); f.success() }),
                onEnd = listOf(CompiledSkillMechanic { marks.add("end"); f.success() }), hitCondition = filter)
            registration?.close()
            registration = f.register("launch") { _, _ -> CompiledSkillMechanic { c -> manager.forContext(c as PlatformExecutionContext).launch(c.invocation(), options, hooks).thenApply { handle = it as ProjectileInstance; SkillResult.success() } } }
            f.write(effects = "  - type: launch"); f.load()
            assertEquals(expectSuccess, f.cast(input = f.invocation().derive().origin(point(0.0)).location(point(0.0)).target(victim).build()).join().isSuccess)
        }
    }
    @Test fun fastSweptHitsAreOrderedAndPiercingCannotExceedTotalHits() {
        val r = Rig(); val other = SkillActor.of(r.f.legacy.caster)
        r.contacts = { _, _ -> listOf(r.hit(3.0, .75, other), r.hit(1.0, .25)) }
        r.launch(ProjectileOptions(speed = 4.0, direction = SkillVector(1.0, 0.0, 0.0), lifetimeTicks = 10, pierce = 10, maxHits = 1, hitCaster = true))
        r.f.scheduler.advance(1); assertEquals(listOf(r.victim), r.hits); assertFalse(r.handle.active()); assertEquals(1, r.cleanups); assertEquals(1, r.marks.count { it == "end" })
        r.f.scheduler.advance(100); assertEquals(1, r.hits.size)
    }
    @Test fun movementWaitsForCollisionSnapshotAndUnloadDiscardsLateResults() {
        val r = Rig(); val pending = CompletableFuture<List<SkillCollision>>(); r.barrier = pending; r.launch()
        r.f.scheduler.advance(1); assertEquals(r.point(0.0), r.handle.position)
        r.f.engine.closeOwner(r.f.legacy.plugin); pending.complete(listOf(r.hit(1.0, .25)))
        assertFalse(r.handle.active()); assertTrue(r.hits.isEmpty()); assertEquals(1, r.cleanups); assertFalse("end" in r.marks); assertEquals(0, r.f.platform.managed.size)
    }
    @Test fun unloadedPathAndRetiredHomingTargetStopWithoutCollisionOrChunkLoading() {
        val r = Rig(); r.launch(); r.loaded = false; r.f.scheduler.advance(1)
        assertFalse(r.handle.active()); assertEquals(0, r.calls); assertEquals(r.point(0.0), r.handle.position)
    }
    @Test fun homingUsesTargetSnapshotsAndStopsWhenTargetRetires() {
        val r = Rig(); r.launch(ProjectileOptions(speed = 2.0, direction = SkillVector(1.0, 0.0, 0.0), homing = 1.0, lifetimeTicks = 10))
        r.f.scheduler.advance(1); assertTrue(r.handle.position.z() > 1.0)
        r.retired = true; r.f.scheduler.advance(1); assertFalse(r.handle.active()); assertEquals(SkillRemovalReason.ACTOR_RETIRED, r.handle.reason().orElseThrow())
    }
    @Test fun bounceReflectsDirectionAndEnergyWhileHitImmunitySuppressesRepeatedTargets() {
        val r = Rig(); var wall = true
        r.contacts = { _, _ -> if (wall) { wall = false; listOf(r.hit(2.0, .5, null)) } else emptyList() }
        r.launch(ProjectileOptions(speed = 4.0, direction = SkillVector(1.0, 0.0, 0.0), bounces = 1, bounceEnergy = .5, lifetimeTicks = 20))
        r.f.scheduler.advance(1); assertEquals(-2.0, r.handle.velocity.x(), 1e-8); assertTrue(r.handle.position.x() < 2.0)
        r.contacts = { _, _ -> listOf(r.hit(r.handle.position.x() - .1, .1)) }
        repeat(3) { r.f.scheduler.advance(1) }; assertEquals(1, r.hits.count { it == r.victim })
        r.handle.remove(SkillRemovalReason.DISPELLED); assertEquals(0, r.f.platform.managed.size)
    }
    @Test fun seededMotionGravityFiniteBoundsAndLedgerAreDeterministic() {
        val options = ProjectileOptions(speed = 3.0, direction = SkillVector(1.0, 0.0, 0.0), spreadDegrees = 12.0, seed = 42, gravity = .2)
        assertEquals(ProjectileMotion.initial(options), ProjectileMotion.initial(options))
        val velocity = ProjectileMotion.advance(SkillVector(1.0, 0.0, 0.0), options, null)
        assertEquals(-.2, velocity.y(), 1e-9)
        assertThrows(IllegalArgumentException::class.java) { options.copy(speed = Double.NaN) }
        assertThrows(IllegalArgumentException::class.java) { options.copy(lifetimeTicks = 0) }
        val ledger = ProjectileHitLedger(2, 5); val id = UUID.randomUUID()
        assertTrue(ledger.accept(id, 1)); assertFalse(ledger.accept(id, 5)); assertTrue(ledger.accept(id, 6)); assertFalse(ledger.accept(UUID.randomUUID(), 7))
    }
    @Test fun reloadCancelsFlightAndConcurrentRemovalRunsCleanupOnce() {
        val r = Rig(); r.launch(); r.f.load(); assertFalse(r.handle.active()); assertEquals(SkillRemovalReason.DEFINITION_RELOADED, r.handle.reason().orElseThrow()); assertEquals(1, r.cleanups)
        r.launch(); val jobs = (1..8).map { CompletableFuture.runAsync { r.handle.remove(SkillRemovalReason.DISPELLED) } }; CompletableFuture.allOf(*jobs.toTypedArray()).join()
        r.f.scheduler.advance(100); assertEquals(2, r.cleanups); assertEquals(1, r.marks.count { it == "end" }); assertEquals(0, r.f.platform.managed.size)
    }
    @Test fun hitFiltersRunOnCapturedActualTargetAndExpiryCannotRepeatEnd() {
        val r = Rig(); r.contacts = { _, _ -> listOf(r.hit(1.0, .25)) }
        r.launch(ProjectileOptions(speed = 4.0, direction = SkillVector(1.0, 0.0, 0.0), lifetimeTicks = 3, pierce = 5), SkillPredicate { assertEquals(r.victim, it.target().orElseThrow()); false })
        repeat(3) { r.f.scheduler.advance(1) }; assertTrue(r.hits.isEmpty()); assertEquals(1, r.marks.count { it == "end" }); assertEquals(1, r.cleanups)
    }
    @Test fun aRetiredContactCannotReceiveHitHooksOrConsumePiercing() {
        val r = Rig(); r.contacts = { _, _ -> listOf(r.hit(1.0, .25)) }; r.launch()
        every { r.f.legacy.victim.isValid } answers { r.f.scheduler.assertOwner(r.f.legacy.victim); false }
        r.f.scheduler.advance(1); assertTrue(r.hits.isEmpty()); assertTrue(r.handle.active())
        r.f.engine.shutdown(); assertEquals(1, r.cleanups)
    }
    @Test fun callbackBudgetIsSharedWithManagedHooksAndNeverResetsEachTick() {
        val r = Rig(); r.preloadCallbacks = 4094; r.launch(); r.f.scheduler.advance(1)
        assertFalse(r.handle.active()); assertEquals(SkillRemovalReason.BUDGET_EXCEEDED, r.handle.reason().orElseThrow()); assertEquals(1, r.cleanups); assertEquals(0, r.f.platform.managed.size)
    }
    @Test fun failedQueryInitializationReleasesTheAdoptedLease() {
        val f = PlatformFixture(directory)
        val manager = ProjectileManager(f.engine, f.platform, queryFactory = { throw IllegalStateException("Unavailable query") })
        f.register("launch") { _, _ -> CompiledSkillMechanic { c -> manager.forContext(c as PlatformExecutionContext).launch(c.invocation(), ProjectileOptions(), ProjectileHooks()).thenApply { SkillResult.success() } } }
        f.write(effects = "  - type: launch"); f.load(); assertFalse(f.cast().join().isSuccess)
        assertEquals(0, f.platform.managed.size); assertEquals(0, manager.size)
    }
    @Test fun targetCancellationAndClosedManagerCannotLeaveLiveFlights() {
        val r = Rig(); r.launch(); r.f.engine.cancel(r.victim.id())
        assertFalse(r.handle.active()); assertEquals(1, r.cleanups)
        r.manager.close(); r.launch(expectSuccess = false); assertEquals(0, r.f.platform.managed.size)
    }
}
