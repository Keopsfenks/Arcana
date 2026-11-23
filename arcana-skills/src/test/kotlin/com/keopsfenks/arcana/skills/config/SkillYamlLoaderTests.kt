package com.keopsfenks.arcana.skills.config

import com.keopsfenks.arcana.skills.compile.CompilerFixture
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class SkillYamlLoaderTests {
    @TempDir lateinit var directory: Path
    private val yaml = """
        id: sample
        triggers: [attack]
        cooldown: 2s
        effects:
          - type: mark
    """.trimIndent()

    private fun load() = SkillYamlLoader { CompilerFixture().compiler() }.load(directory)

    @Test fun `directory compilation retains disabled definitions ignores samples and builds a trigger index`() {
        Files.writeString(directory.resolve("first.yml"), yaml)
        Files.createDirectories(directory.resolve("nested"))
        Files.writeString(directory.resolve("nested/disabled.yml"), yaml.replace("sample", "disabled") + "\nenabled: false")
        Files.writeString(directory.resolve("_example.yml"), "this is not yaml: [")
        val loaded = load()
        assertTrue(loaded.report.success(), loaded.report.errors().toString())
        assertEquals(2, loaded.report.loaded())
        assertEquals(1, loaded.report.disabled())
        assertEquals(listOf("sample"), loaded.snapshot!!.byTrigger["attack"]!!.map { it.id })
        assertThrows(UnsupportedOperationException::class.java) {
            (loaded.snapshot!!.byId as MutableMap).clear()
        }
    }

    @Test fun `any bad definition rejects the whole candidate with its path`() {
        Files.writeString(directory.resolve("good.yml"), yaml)
        Files.writeString(directory.resolve("bad.yml"), yaml.replace("mark", "unknown"))
        val loaded = load()
        assertFalse(loaded.report.success())
        assertNull(loaded.snapshot)
        assertTrue(loaded.report.errors().any { it.contains("bad.yml") && it.contains("effects[0]") })
    }

    @Test fun `duplicate identities missing ids invalid types and unknown triggers reject`() {
        Files.writeString(directory.resolve("first.yml"), yaml)
        Files.writeString(directory.resolve("second.yml"), yaml)
        assertFalse(load().report.success())
        Files.delete(directory.resolve("second.yml"))
        for (invalid in listOf(
            yaml.replace("id: sample", ""), yaml.replace("triggers: [attack]", "triggers: attack"),
            yaml.replace("triggers: [attack]", "triggers: [missing]"), yaml + "\nchance: 101",
            yaml + "\nenabled: perhaps", yaml.replace("effects:\n  - type: mark", "effects: wrong")
        )) {
            Files.writeString(directory.resolve("first.yml"), invalid)
            assertFalse(load().report.success(), invalid)
        }
    }

    @Test fun `unsafe tags duplicate keys and nonfinite numbers are rejected`() {
        for (invalid in listOf("!!java.net.URL [https://example.com]", yaml + "\nid: another", yaml + "\nchance: .nan")) {
            Files.writeString(directory.resolve("bad.yml"), invalid)
            assertFalse(load().report.success(), invalid)
        }
    }
}
