package com.keopsfenks.arcana.skills.components

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.effects.projectiles.*
import com.keopsfenks.arcana.skills.components.effects.variables.*
import io.mockk.*
import org.bukkit.*
import org.bukkit.util.BoundingBox
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.nio.file.Files

class ProjectileMechanicTests {
    @TempDir lateinit var directory: Path
    private val fixtures = mutableListOf<NativeComponentFixture>()
    @BeforeEach fun mockWorldResolver() { mockkStatic(Bukkit::class) }
    @AfterEach fun cleanup() { fixtures.forEach { it.engine.shutdown() }; unmockkStatic(Bukkit::class) }
    private fun fixture(): NativeComponentFixture {
        val f = NativeComponentFixture(directory); fixtures.add(f)
        f.install(listOf(ProjectileMechanic(), MissileMechanic(), RayMechanic(), BeamMechanic(), ChainMechanic(), SetVariableMechanic(), AddVariableMechanic()))
        every { Bukkit.getWorld(f.worldId) } answers { f.scheduler.assertOwner("global"); f.root.world }
        fun chunkOwner(x: Int, z: Int) { val location = f.scheduler.current as? Location ?: error("Foreign chunk access"); assertEquals(x, location.blockX shr 4); assertEquals(z, location.blockZ shr 4) }
        val chunk = mockk<Chunk>(); every { chunk.entities } answers { assertTrue(f.scheduler.current is Location); arrayOf(f.root.caster, f.root.victim) }
        every { f.root.world.isChunkLoaded(any<Int>(), any<Int>()) } answers { chunkOwner(firstArg(), secondArg()); true }
        every { f.root.world.getChunkAt(any<Int>(), any<Int>()) } answers { chunkOwner(firstArg(), secondArg()); chunk }
        every { f.root.world.getNearbyEntities(any<BoundingBox>()) } answers { assertTrue(f.scheduler.current is Location); listOf(f.root.caster, f.root.victim) }
        every { f.root.world.minHeight } returns 0; every { f.root.world.maxHeight } returns 256
        val air = mockk<org.bukkit.block.Block>(); val shape = mockk<org.bukkit.util.VoxelShape>()
        every { shape.boundingBoxes } returns emptyList(); every { air.collisionShape } returns shape
        every { f.root.world.getBlockAt(any<Int>(), any<Int>(), any<Int>()) } answers { chunkOwner(firstArg<Int>() shr 4, thirdArg<Int>() shr 4); air }
        return f
    }
    @Test fun fiveFactoriesHaveDistinctIdsAndProjectileNamedHitReceivesActualTargetOriginAndParameters() {
        val f = fixture(); var power = 0.0; var victim: SkillActor? = null; var origin: SkillLocation? = null
        f.register("record_hit") { _, _ -> CompiledSkillMechanic { c -> val input = c.invocation(); power = input.parameters().getValue("power").number(); victim = input.target().orElse(null); origin = input.origin().orElse(null); f.success() } }
        f.write("hit", "parameters:\n  power: {type: number}", "  - type: record_hit")
        f.write(options = "parameters:\n  power: {type: number, default: 9}", effects = "  - {type: projectile, speed: 4, direction: {x: 1, y: 0, z: 0}, duration: 10t, on-hit: hit, on-end: [{type: mark, name: ended}]}")
        f.load(); assertTrue(f.cast().join().isSuccess); f.scheduler.advance(1)
        assertEquals(9.0, power); assertEquals(SkillActor.of(f.root.victim), victim); assertEquals(SkillLocation(f.worldId, 0.0, 60.0, 0.0), origin)
        assertEquals(listOf("ended"), f.marks); assertEquals(0, f.platform.managed.size)
        assertEquals(setOf("projectile", "missile", "ray", "beam", "chain"), listOf(ProjectileMechanic(), MissileMechanic(), RayMechanic(), BeamMechanic(), ChainMechanic()).map { it.descriptor().id() }.toSet())
    }
    @Test fun rayAndChainUseFreshActualVictimsAndNeverRevisitOneTarget() {
        val f = fixture(); val targets = mutableListOf<SkillActor>()
        f.register("record_hit") { _, _ -> CompiledSkillMechanic { c -> c.invocation().target().ifPresent(targets::add); f.success() } }
        f.write("ray", effects = "  - {type: ray, length: 8, direction: {x: 1, y: 0, z: 0}, on-hit: [{type: record_hit}]}")
        f.write("chain", effects = "  - {type: chain, radius: 8, jumps: 100, on-hit: [{type: record_hit}]}")
        f.load(); assertTrue(f.cast("ray").join().isSuccess); assertEquals(listOf(SkillActor.of(f.root.victim)), targets)
        targets.clear(); val input = f.invocation().derive().target(SkillActor.of(f.root.victim)).build()
        assertTrue(f.platform.cast(SkillKey("arcana", "chain"), input).toCompletableFuture().join().isSuccess)
        assertEquals(listOf(SkillActor.of(f.root.victim)), targets); verify(exactly = 0) { f.root.world.entities }
    }
    @Test fun beamSharesManagedScopeAndStopsAllPulsesAfterSourceRetirement() {
        val f = fixture(); var hits = 0
        f.register("record_hit") { _, _ -> CompiledSkillMechanic { hits++; f.success() } }
        f.write(effects = "  - {type: beam, length: 8, direction: {x: 1, y: 0, z: 0}, duration: 10t, period: 2t, on-hit: [{type: record_hit}], on-end: [{type: mark, name: end}]}")
        f.load(); f.cast().join(); assertEquals(1, hits); f.scheduler.advance(2); assertEquals(2, hits)
        f.engine.closeOwner(f.root.plugin); f.scheduler.advance(100); assertEquals(2, hits); assertTrue(f.marks.isEmpty()); assertEquals(0, f.platform.managed.size)
    }
    @Test fun missileRequiresATargetAndHitConditionsCanRejectContacts() {
        val f = fixture(); var hits = 0
        f.register("record_hit") { _, _ -> CompiledSkillMechanic { hits++; f.success() } }
        f.platform.register(f.root.plugin, SkillComponentType.PREDICATE, SkillKey("arcana", "deny"), object : SkillPredicateType(SkillDescriptor.of("deny", "Test contact filter")) {
            override fun compile(arguments: SkillArguments, compilation: SkillCompilation) = SkillPredicate { false }
        })
        f.write("missile", effects = "  - {type: missile, speed: 4, duration: 10t, on-hit: [{type: record_hit}]}")
        f.write("ray", effects = "  - {type: ray, length: 8, direction: {x: 1, y: 0, z: 0}, hit-conditions: [{type: deny}], on-hit: [{type: record_hit}]}")
        f.load(); assertEquals(SkillResult.Status.NO_TARGET, f.cast("missile").join().status()); assertTrue(f.cast("ray").join().isSuccess); assertEquals(0, hits)
        assertTrue(f.platform.cast(SkillKey("arcana", "missile"), f.invocation().derive().target(SkillActor.of(f.root.victim)).build()).toCompletableFuture().join().isSuccess)
        f.scheduler.advance(1); assertEquals(1, hits)
    }
    @Test fun invalidGeometryUnknownHooksAndExcessiveChainRejectWithNodePaths() {
        val f = fixture()
        for (effect in listOf("{type: projectile, speed: 0, duration: 10t}", "{type: projectile, direction: {x: 0, y: 0, z: 0}, duration: 10t}", "{type: beam, duration: 0t}", "{type: ray, length: 257}", "{type: chain, jumps: 101}", "{type: projectile, duration: 10t, on-hit: missing}", "{type: ray, on-hitt: []}")) {
            f.write(effects = "  - $effect"); val report = f.engine.reload(); assertFalse(report.success(), effect); assertTrue(report.errors().joinToString().contains("effects[0]"), report.errors().toString())
        }
    }
    @Test fun packagedBoltBeamAndChainAreDisabledAndCompileIndependently() {
        val f = fixture()
        for (name in listOf("projectile-bolt", "projectile-beam", "projectile-chain")) {
            val text = requireNotNull(javaClass.getResourceAsStream("/arcana-skills/examples/v2/$name.yml")).bufferedReader().use { it.readText() }
            assertTrue(text.contains("enabled: false")); Files.writeString(f.root.directory.resolve("$name.yml"), text.replace("enabled: false", "enabled: true"))
            f.load(); Files.delete(f.root.directory.resolve("$name.yml"))
        }
    }
    @Test fun aWallStopsRayEvenWhenItsHitPredicateRejectsTheBlock() {
        val f = fixture(); var hits = 0
        val wall = mockk<org.bukkit.block.Block>(); val air = mockk<org.bukkit.block.Block>()
        val solid = mockk<org.bukkit.util.VoxelShape>(); val empty = mockk<org.bukkit.util.VoxelShape>()
        every { solid.boundingBoxes } returns listOf(BoundingBox(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)); every { empty.boundingBoxes } returns emptyList()
        every { wall.collisionShape } returns solid; every { air.collisionShape } returns empty
        every { f.root.world.getBlockAt(any<Int>(), any<Int>(), any<Int>()) } answers { assertTrue(f.scheduler.current is Location); if (firstArg<Int>() == 1) wall else air }
        f.platform.register(f.root.plugin, SkillComponentType.PREDICATE, SkillKey("arcana", "entity_only"), object : SkillPredicateType(SkillDescriptor.of("entity_only", "Require actual entity contact")) {
            override fun compile(arguments: SkillArguments, compilation: SkillCompilation) = SkillPredicate { it.target().isPresent }
        })
        f.register("record_hit") { _, _ -> CompiledSkillMechanic { hits++; f.success() } }
        f.write(effects = "  - {type: ray, length: 8, direction: {x: 1, y: 0, z: 0}, hit-conditions: [{type: entity_only}], on-hit: [{type: record_hit}]}")
        f.load(); assertTrue(f.cast().join().isSuccess); assertEquals(0, hits)
    }
    @Test fun recursiveProjectileHitCastsKeepTheOriginalCallDepthAndRetireAllFlights() {
        val f = fixture(); f.install(listOf(com.keopsfenks.arcana.skills.components.effects.flow.CastMechanic()))
        f.write(effects = "  - {type: projectile, speed: 4, direction: {x: 1, y: 0, z: 0}, duration: 100t, on-hit: [{type: mark, name: hit}, {type: cast, skill: one}]}")
        f.load(); assertTrue(f.cast().join().isSuccess)
        repeat(25) { f.scheduler.advance(1) }
        assertTrue(f.marks.size in 16..17, f.marks.toString()); assertEquals(0, f.platform.managed.size); assertEquals(0, f.platform.projectiles.size)
    }
    @Test fun beamTotalHitLimitSurvivesRepeatedPulsesAndNormalExpiryEndsOnce() {
        val f = fixture()
        f.write(effects = "  - {type: beam, length: 8, direction: {x: 1, y: 0, z: 0}, duration: 5t, period: 1t, max-hits: 1, on-hit: [{type: mark, name: hit}], on-end: [{type: mark, name: end}]}")
        f.load(); f.cast().join(); repeat(10) { f.scheduler.advance(1) }
        assertEquals(listOf("hit", "end"), f.marks); assertEquals(0, f.platform.managed.size)
    }
}
