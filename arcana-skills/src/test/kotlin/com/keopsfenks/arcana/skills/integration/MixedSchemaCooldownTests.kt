package com.keopsfenks.arcana.skills.integration
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.execution.PlatformFixture
import com.keopsfenks.arcana.skills.execution.CooldownStore
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class MixedSchemaCooldownTests {
    @Test fun bothSchemasAndMigrationShareArcanaCooldownWithoutMixingOtherNamespaces(@TempDir dir: Path) {
        val f = PlatformFixture(dir); f.register("native_mark") { _, _ -> CompiledSkillMechanic { f.success() } }
        f.legacy.write(id = "old", options = "cooldown: 10s\ncooldown-group: dash")
        f.write(options = "cooldown: 10s\ncooldown-group: dash"); f.load()
        assertTrue(f.legacy.cast("old").join().isSuccess)
        assertEquals(SkillResult.Status.COOLDOWN, f.cast().join().status())
        f.legacy.clock.addAndGet(10_000_000_000)
        assertTrue(f.cast().join().isSuccess)
        assertEquals(SkillResult.Status.COOLDOWN, f.legacy.cast("old").join().status())
        f.write(id = "old", options = "cooldown: 10s\ncooldown-group: dash"); f.load()
        assertEquals(SkillResult.Status.COOLDOWN, f.cast("old").join().status())
        assertTrue(f.engine.cooldowns.tryAcquire(f.legacy.playerId, "alpha:dash", 10_000_000_000))
        assertTrue(f.engine.cooldowns.remaining(f.legacy.playerId, "dash") > 0)
        assertEquals(f.engine.cooldowns.remaining(f.legacy.playerId, "dash"), f.engine.cooldowns.remaining(f.legacy.playerId, "arcana:dash")); f.engine.shutdown()
    }
}
