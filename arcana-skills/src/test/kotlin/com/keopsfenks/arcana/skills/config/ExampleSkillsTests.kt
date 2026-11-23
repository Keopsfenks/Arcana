package com.keopsfenks.arcana.skills.config

import com.keopsfenks.arcana.skills.SkillsRuntime
import com.keopsfenks.arcana.skills.execution.EngineFixture
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ExampleSkillsTests {
    @TempDir lateinit var directory: Path

    @Test fun `examples remain inactive and existing edited sample files are not overwritten`() {
        val f = EngineFixture(directory)
        Files.writeString(directory.resolve("_welcome.yml"), "my local edits")
        val runtime = SkillsRuntime(f.plugin, f.engine, directory)
        runtime.start()
        assertEquals("my local edits", Files.readString(directory.resolve("_welcome.yml")))
        assertTrue(f.engine.skills().isEmpty())
        assertTrue(Files.readString(directory.resolve("_vampiric_strike.yml")).contains("enabled: false"))
        assertTrue(Files.readString(directory.resolve("_combo.yml")).contains("enabled: false"))
        runtime.stop()
    }
}
