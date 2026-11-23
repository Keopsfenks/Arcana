package com.keopsfenks.arcana.skills.execution

import com.keopsfenks.arcana.skills.BuiltinComponents
import io.mockk.every
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class DamageRecursionTests {
    @TempDir lateinit var directory: Path

    @Test fun `generated damage is marked synchronously and marker clears even when damage throws`() {
        val f = EngineFixture(directory)
        BuiltinComponents.install(f.engine, f.plugin)
        var calls = 0
        every { f.victim.damage(any<Double>()) } answers {
            assertTrue(SkillDamageScope.isActive())
            f.scheduler.assertOwner(f.victim)
            calls++
            throw IllegalStateException("event listener failed")
        }
        f.write(effects = "  - type: damage\n    amount: 2\n    target: victim")
        f.load()
        assertFalse(f.cast(target = true).join().isSuccess)
        assertEquals(1, calls)
        assertFalse(SkillDamageScope.isActive())
    }
}
