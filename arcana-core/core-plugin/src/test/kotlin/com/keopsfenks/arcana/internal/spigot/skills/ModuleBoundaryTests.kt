package com.keopsfenks.arcana.internal.spigot.skills

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.internal.spigot.math.api.ArcanaExpressionEnvironmentBuilder
import com.keopsfenks.arcana.skills.BuiltinComponents
import com.keopsfenks.arcana.skills.DefaultSkillEngine
import com.keopsfenks.arcana.skills.SkillsRuntime
import com.keopsfenks.arcana.skills.compile.SkillCompilerImpl
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ModuleBoundaryTests {
    @TempDir lateinit var directory: Path

    @Test fun runtimeNeedsNoCatalogClasses() {
        val plugin = mockk<ArcanaPlugin>(relaxed = true)
        val environment = ArcanaExpressionEnvironmentBuilder().variables(*SkillCompilerImpl.VARIABLES).build()
        val engine = DefaultSkillEngine(plugin, directory, environment)
        val runtime = SkillsRuntime(plugin, engine, directory) { _, _ -> }
        try {
            runtime.start()
            assertTrue(runtime.isStarted)
            assertTrue(engine.effects.snapshot().isEmpty())
            assertTrue(engine.triggers.snapshot().isEmpty())
            assertTrue(Files.exists(directory.resolve("_welcome.yml")))
        } finally { runtime.stop() }
    }

    @Test fun coreInstallsCatalogExactlyOnce() {
        val plugin = mockk<ArcanaPlugin>(relaxed = true)
        val environment = ArcanaExpressionEnvironmentBuilder().variables(*SkillCompilerImpl.VARIABLES).build()
        val engine = DefaultSkillEngine(plugin, directory, environment)
        var installs = 0
        val runtime = SkillsRuntime(plugin, engine, directory) { target, owner ->
            installs++
            BuiltinComponents.install(target, owner)
        }
        try {
            runtime.start()
            runtime.start()
            assertEquals(1, installs)
            assertEquals(18, engine.effects.snapshot().size)
            Files.writeString(directory.resolve("manual.yml"), "id: manual\neffects:\n  - type: heal\n    amount: 2\n")
            assertTrue(engine.reload().success())
            assertEquals("manual", engine.skills().single().id())
            assertNotEquals(DefaultSkillEngine::class.java.protectionDomain.codeSource.location,
                BuiltinComponents::class.java.protectionDomain.codeSource.location)
        } finally { runtime.stop() }
    }
}
