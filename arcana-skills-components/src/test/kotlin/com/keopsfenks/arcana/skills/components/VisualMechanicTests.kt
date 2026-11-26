package com.keopsfenks.arcana.skills.components
import com.keopsfenks.arcana.core.particle.SpawnableParticle
import com.keopsfenks.arcana.core.particle.impl.EmptyParticle
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.effects.visual.*
import io.mockk.*
import org.bukkit.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

class VisualMechanicTests {
    @TempDir lateinit var directory: Path
    private val fixtures = mutableListOf<NativeComponentFixture>()
    @BeforeEach fun resolver() { mockkStatic(Bukkit::class) }
    @AfterEach fun cleanup() { fixtures.forEach { it.engine.shutdown() }; unmockkStatic(Bukkit::class) }
    private fun fixture() = NativeComponentFixture(directory).also { f ->
        fixtures.add(f); every { Bukkit.getWorld(f.worldId) } answers { f.scheduler.assertOwner("global"); f.root.world }
        every { f.root.world.isChunkLoaded(any<Int>(), any<Int>()) } answers { val location = f.scheduler.current as? Location ?: error("Chunk check requires location owner"); assertEquals(firstArg<Int>(), location.blockX shr 4); assertEquals(secondArg<Int>(), location.blockZ shr 4); true }
    }
    @Test fun particleLookupCompilesOnceAndLineRingGeometryIsFiniteAndBounded() {
        val f = fixture(); var resolutions = 0; val positions = mutableListOf<Location>(); var amount = 0
        val particle = mockk<SpawnableParticle>(); every { particle.spawn(any<Location>(), any<Int>()) } answers { val at = firstArg<Location>(); val current = f.scheduler.current as? Location ?: error("Foreign particle access"); assertEquals(at.blockX shr 4, current.blockX shr 4); positions.add(at.clone()); amount += secondArg<Int>(); Unit }
        val resolve: (String) -> SpawnableParticle = { resolutions++; particle }
        f.install(listOf(ParticleMechanic(resolve), ParticleLineMechanic(resolve), ParticleRingMechanic(resolve)))
        f.write(effects = "  - {type: particle_line, particle: 'test:spark', points: 3, length: 4, direction: {x: 1, y: 0, z: 0}, amount: 1}\n  - {type: particle_ring, particle: 'test:spark', points: 4, radius: 2, amount: 2}\n  - {type: particle, particle: 'test:spark', amount: 1}")
        f.load(); assertEquals(3, resolutions); f.cast().join(); f.cast().join(); assertEquals(3, resolutions); assertEquals(16, positions.size); assertEquals(24, amount)
        assertEquals(listOf(0.0, 2.0, 4.0), positions.take(3).map { it.x }); assertTrue(positions.all { it.x.isFinite() && it.y.isFinite() && it.z.isFinite() })
    }
    @Test fun invalidParticleGeometryAndUnknownLookupRejectBeforePublication() {
        val f = fixture(); f.install(listOf(ParticleMechanic { EmptyParticle() }, ParticleLineMechanic { mockk() }))
        for (effect in listOf("{type: particle, particle: 'bad:missing'}", "{type: particle_line, particle: 'test:spark', points: 257}", "{type: particle_line, particle: 'test:spark', points: 256, amount: 100}")) {
            f.write(effects = "  - $effect"); assertFalse(f.engine.reload().success(), effect)
        }
    }
    @Test fun soundOnTwoSelectedActorsUsesEachLocationAndStopSoundRequiresPlayer() {
        val f = fixture(); val heard = mutableListOf<Double>()
        f.install(listOf(SoundMechanic(), StopSoundMechanic()))
        f.platform.register(f.root.plugin, SkillComponentType.TARGETER, SkillKey("arcana", "both"), object : SkillTargeter(SkillDescriptor.of("both", "Two owned actors")) {
            override fun compile(arguments: SkillArguments, compilation: SkillCompilation) = CompiledSkillTargeter { CompletableFuture.completedFuture(SkillSelection(listOf(SkillActor.of(f.root.caster), SkillActor.of(f.root.victim)), emptyList())) }
        })
        every { f.root.world.playSound(any<Location>(), any<String>(), any<Float>(), any<Float>()) } answers { assertTrue(f.scheduler.current is Location); heard.add(firstArg<Location>().x); Unit }
        every { f.root.caster.stopSound(any<String>()) } answers { f.scheduler.assertOwner(f.root.caster); Unit }
        f.write(effects = "  - {type: sound, targeter: both, sound: entity.player.levelup}"); f.write("stop", effects = "  - {type: stop_sound, sound: entity.player.levelup}"); f.load()
        assertTrue(f.cast().join().isSuccess); assertEquals(listOf(0.0, 3.0), heard)
        assertTrue(f.cast("stop").join().isSuccess); assertEquals(SkillResult.Status.UNSUPPORTED_ACTOR, f.cast("stop", true).join().status())
    }
}
