package com.keopsfenks.arcana.skills.integration

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.SkillReloadCoordinator
import com.keopsfenks.arcana.skills.SkillsLifecycleListener
import com.keopsfenks.arcana.skills.SkillsRuntime
import com.keopsfenks.arcana.skills.execution.EngineFixture
import io.mockk.every
import io.mockk.mockk
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.event.server.PluginEnableEvent
import org.bukkit.plugin.Plugin
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

class SkillLifecycleTests {
    @TempDir lateinit var directory: Path

    @Test fun `global reload coordinator stages without mutation and rejects bad files before cancelling jobs`() {
        val f = EngineFixture(directory)
        f.write(effects = "  - type: wait\n  - type: mark")
        val reports = mutableListOf<SkillReloadResult>()
        val coordinator = SkillReloadCoordinator(f.engine) { reports.add(it) }
        assertTrue(coordinator.initialize().success())
        val pending = f.cast()
        Files.writeString(directory.resolve("bad.yml"), "bad: [")
        assertFalse(coordinator.prepare())
        assertFalse(pending.isDone)
        assertNull(coordinator.commit())
        Files.delete(directory.resolve("bad.yml"))
        assertTrue(coordinator.prepare())
        assertFalse(pending.isDone)
        assertTrue(coordinator.commit()!!.success())
        assertEquals(SkillResult.Status.CANCELLED, pending.join().status())
    }

    @Test fun `invalid first boot can keep the framework running with an empty skill set`() {
        val f = EngineFixture(directory)
        Files.writeString(directory.resolve("bad.yml"), "bad: [")
        val coordinator = SkillReloadCoordinator(f.engine) {}
        assertFalse(coordinator.initialize().success())
        assertTrue(coordinator.prepare(allowEmptyStartup = true))
        assertNull(coordinator.commit())
        assertTrue(f.engine.skills().isEmpty())
        assertFalse(coordinator.prepare())
    }

    @Test fun `quit and external owner disable reach the engines cancellation and registration ownership`() {
        val f = EngineFixture(directory)
        val owner = mockk<Plugin>()
        fun effect() = object : SkillEffect("external") {
            override fun compile(arguments: SkillArguments, compiler: SkillCompiler) =
                CompiledSkillEffect { CompletableFuture.completedFuture(SkillResult.success()) }
        }
        f.engine.registerEffect(owner, effect())
        f.write(effects = "  - type: wait\n  - type: external")
        f.load()
        val listener = SkillsLifecycleListener(f.engine)
        val pending = f.cast()
        val disabled = mockk<PluginDisableEvent>()
        every { disabled.plugin } returns owner
        listener.onPluginDisable(disabled)
        assertEquals(SkillResult.Status.CANCELLED, pending.join().status())
        assertThrows(IllegalStateException::class.java) { f.engine.registerEffect(owner, effect()) }
        val enabled = mockk<PluginEnableEvent>()
        every { enabled.plugin } returns owner
        listener.onPluginEnable(enabled)
        f.engine.registerEffect(owner, effect())
        f.load()
        val logout = f.cast()
        val quit = mockk<PlayerQuitEvent>()
        every { quit.player } returns f.caster
        listener.onQuit(quit)
        assertEquals(SkillResult.Status.CANCELLED, logout.join().status())
    }

    @Test fun `runtime start is idempotent and copies only inactive examples`() {
        val f = EngineFixture(directory)
        val runtime = SkillsRuntime(f.plugin, f.engine, directory)
        runtime.start()
        runtime.start()
        assertTrue(runtime.isStarted)
        assertTrue(Files.exists(directory.resolve("_welcome.yml")))
        assertTrue(f.engine.skills().isEmpty())
        f.write("manual")
        assertTrue(f.engine.reload().success())
        assertTrue(f.cast("manual").join().isSuccess)
        runtime.stop()
        runtime.stop()
        assertFalse(runtime.isStarted)
    }
}
