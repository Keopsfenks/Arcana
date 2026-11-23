package com.keopsfenks.arcana.skills.integration

import com.keopsfenks.arcana.skills.commands.SkillPlayers
import com.keopsfenks.arcana.skills.commands.SkillsCommandActions
import com.keopsfenks.arcana.skills.execution.EngineFixture
import io.mockk.every
import io.mockk.mockk
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class SkillsCommandTests {
    @TempDir lateinit var directory: Path

    @Test fun `console needs an explicit player permission gates players and casts report their outcome`() {
        val f = EngineFixture(directory)
        f.write()
        f.load()
        val messages = mutableListOf<String>()
        val players = object : SkillPlayers {
            override fun find(name: String): Player? = if (name == "Caster") f.caster else null
            override fun names() = listOf("Caster")
        }
        val actions = SkillsCommandActions(f.plugin, { f.engine }, players) { _, text -> messages.add(text) }
        val console = mockk<CommandSender>()
        actions.cast(console, listOf("one"))
        assertTrue(messages.last().contains("player", ignoreCase = true))
        assertTrue(f.marks.isEmpty())
        actions.cast(console, listOf("one", "Caster"))
        assertEquals(listOf("mark"), f.marks)
        assertTrue(messages.last().contains("SUCCESS"))
        every { f.caster.hasPermission(SkillsCommandActions.PERMISSION) } returns false
        actions.cast(f.caster, listOf("one"))
        assertEquals(1, f.marks.size)
        assertTrue(actions.castSuggestions(f.caster, listOf("o")).isEmpty())
        every { f.caster.hasPermission(SkillsCommandActions.PERMISSION) } returns true
        assertEquals(listOf("one"), actions.castSuggestions(f.caster, listOf("o")))
        assertEquals(listOf("Caster"), actions.castSuggestions(f.caster, listOf("one", "C")))
    }

    @Test fun `reload reports diagnostics without claiming success and list includes retained definitions`() {
        val f = EngineFixture(directory)
        f.write()
        f.load()
        val messages = mutableListOf<String>()
        val actions = SkillsCommandActions(f.plugin, { f.engine }, mockk()) { _, text -> messages.add(text) }
        val console = mockk<CommandSender>()
        Files.writeString(directory.resolve("bad.yml"), "bad: [")
        actions.reload(console)
        assertTrue(messages.any { it.contains("bad.yml") })
        assertFalse(messages.any { it.contains("Reloaded") })
        assertEquals(listOf("one"), f.engine.skills().map { it.id() })
        messages.clear()
        actions.list(console)
        assertTrue(messages.any { it.contains("one") })
    }
}
