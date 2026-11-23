package com.keopsfenks.arcana.skills.effects

import com.keopsfenks.arcana.core.particle.SpawnableParticle
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.BuiltinComponents
import com.keopsfenks.arcana.skills.compile.CompilerFixture
import com.keopsfenks.arcana.skills.execution.EngineFixture
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.Location
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class BuiltinEffectTests {
    @TempDir lateinit var directory: Path

    @Test fun `component factories reject bad arguments before any game action`() {
        val compiler = CompilerFixture().compiler()
        val cases = listOf(
            ParticleEffect { mockk<SpawnableParticle>() } to mapOf("particle" to "missing"),
            SoundEffect() to mapOf("sound" to "bad sound"),
            HealEffect() to mapOf("amount" to -1), DamageEffect() to mapOf("amount" to -1),
            PotionEffect { null } to mapOf("potion" to "missing", "duration" to "1s"),
            VelocityEffect() to mapOf("y" to 101), FeedEffect() to mapOf("amount" to 21),
            IgniteEffect() to mapOf("duration" to "-1s"),
            RepeatEffect() to mapOf("times" to 101, "effects" to emptyList<Any>()),
            MessageEffect() to emptyMap<String, Any>(), ActionBarEffect() to emptyMap<String, Any>(),
            ConsoleCommandEffect() to mapOf("command" to ""), PlayerCommandEffect() to mapOf("command" to ""),
            CastEffect() to mapOf("skill" to "bad skill"),
            HealEffect() to mapOf("amount" to 1, "typo" to true)
        )
        for ((effect, values) in cases) {
            assertThrows(IllegalArgumentException::class.java, {
                effect.compile(SkillArguments(values + ("type" to effect.id), "test.effects[0]"), compiler)
            }, effect.id)
        }
    }

    @Test fun `heal clamps health and rejects dead targets`() {
        val f = EngineFixture(directory)
        BuiltinComponents.install(f.engine, f.plugin)
        every { f.caster.health } answers { f.scheduler.assertOwner(f.caster); 18.0 }
        every { f.caster.health = any() } answers { f.scheduler.assertOwner(f.caster) }
        f.write(effects = "  - type: heal\n    amount: 5")
        f.load()
        assertTrue(f.cast().join().isSuccess)
        verify(exactly = 1) { f.caster.health = 20.0 }
        every { f.caster.isDead } returns true
        assertEquals(SkillResult.Status.NO_TARGET, f.cast().join().status())
        verify(exactly = 1) { f.caster.health = any() }
    }

    @Test fun `particle resolves once and spawns through the target location context`() {
        val f = EngineFixture(directory)
        val particle = mockk<SpawnableParticle>()
        var resolved = 0
        every { particle.spawn(any<Location>(), 8) } answers { f.scheduler.assertOwner(firstArg<Location>()) }
        f.engine.registerEffect(f.plugin, ParticleEffect { resolved++; particle })
        f.write(effects = "  - type: particle\n    particle: FLAME\n    amount: 8")
        f.load()
        assertEquals(1, resolved)
        assertTrue(f.cast().join().isSuccess)
        assertTrue(f.cast().join().isSuccess)
        assertEquals(1, resolved)
        verify(exactly = 2) { particle.spawn(any<Location>(), 8) }
    }

    @Test fun `sequence repeat delay and branch preserve order and nested cast applies child permissions`() {
        val f = EngineFixture(directory)
        BuiltinComponents.install(f.engine, f.plugin)
        f.write(effects = """
              - type: sequence
                effects:
                  - type: repeat
                    times: 2
                    interval: 1t
                    effects:
                      - type: mark
                        name: repeated
                  - type: branch
                    condition:
                      type: sneaking
                    then:
                      - type: mark
                        name: 'yes'
                    else:
                      - type: mark
                        name: 'no'
        """.trimIndent().prependIndent("  "))
        f.load()
        val result = f.cast()
        assertEquals(listOf("repeated"), f.marks)
        f.scheduler.advance(1)
        assertTrue(result.join().isSuccess)
        assertEquals(listOf("repeated", "repeated", "no"), f.marks)
        f.write("child", options = "permission: child.use")
        f.write("parent", effects = "  - type: cast\n    skill: child\n  - type: mark\n    name: never")
        f.load()
        every { f.caster.hasPermission("child.use") } returns false
        assertEquals(SkillResult.Status.NO_PERMISSION, f.cast("parent").join().status())
        assertFalse("never" in f.marks)
    }

    @Test fun `literal casts to missing skill files reject the reload`() {
        val f = EngineFixture(directory)
        BuiltinComponents.install(f.engine, f.plugin)
        f.write(effects = "  - type: cast\n    skill: absent")
        assertFalse(f.engine.reload().success())
    }

    @Test fun `plain particle names use canonical enum spelling across server locales`() {
        var resolved = ""
        ParticleEffect { key -> resolved = key; mockk<SpawnableParticle>() }.compile(
            SkillArguments(mapOf("type" to "particle", "particle" to "crit"), "test"), CompilerFixture().compiler()
        )
        assertEquals("CRIT", resolved)
    }

    @Test fun `native delay schedules children and cancels them on reload`() {
        val f = EngineFixture(directory)
        BuiltinComponents.install(f.engine, f.plugin)
        f.write(effects = "  - type: delay\n    duration: 1t\n    effects:\n      - type: mark\n        name: child\n  - type: mark\n    name: after")
        f.load()
        val first = f.cast()
        assertTrue(f.marks.isEmpty())
        f.scheduler.advance(1)
        assertTrue(first.join().isSuccess)
        assertEquals(listOf("child", "after"), f.marks)
        val second = f.cast()
        f.load()
        f.scheduler.advance(1)
        assertEquals(SkillResult.Status.CANCELLED, second.join().status())
        assertEquals(listOf("child", "after"), f.marks)
    }
}
