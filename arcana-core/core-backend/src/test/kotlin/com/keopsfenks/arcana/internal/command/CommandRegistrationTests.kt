package com.keopsfenks.arcana.internal.command

import com.keopsfenks.arcana.core.Arcana
import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.Prerequisite
import com.keopsfenks.arcana.core.command.PluginCommandBase
import com.keopsfenks.arcana.core.scheduling.TaskContext
import io.mockk.*
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandMap
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.util.ArrayDeque

class CommandRegistrationTests {
    private val bridge = mockk<Arcana>(relaxed = true)
    private val plugin = mockk<ArcanaPlugin>(relaxed = true)
    private val map = mockk<CommandMap>()
    private val global = mockk<TaskContext>()
    private val known = linkedMapOf<String, Command>()
    private val queue = ArrayDeque<Runnable>()
    private val snapshots = mutableListOf<Set<String>>()
    private var owned = false
    private var folia = true

    @BeforeEach fun setup() {
        mockkStatic(Arcana::class, Bukkit::class)
        mockkStatic("com.keopsfenks.arcana.internal.command.HandledCommandKt")
        mockkObject(Prerequisite.HAS_FOLIA)
        every { Arcana.get() } returns bridge
        every { Prerequisite.HAS_FOLIA.isMet } answers { folia }
        every { Bukkit.isGlobalTickThread() } answers { owned }
        every { Bukkit.getPluginCommand(any()) } returns null
        every { commandMap } returns map
        every { plugin.name } returns "Arcana"
        every { plugin.scheduler.global() } returns global
        every { global.run(any<Runnable>()) } answers { queue.add(firstArg()); mockk(relaxed = true) }
        every { map.getCommand(any()) } answers { known[firstArg()] }
        every { map.register(any<String>(), any<Command>()) } answers {
            val command = secondArg<Command>(); known[command.name] = command; known["${firstArg<String>()}:${command.name}"] = command; true
        }
        every { bridge.unregisterCommand(any()) } answers {
            val name = firstArg<PluginCommandBase>().name
            val remove = Runnable { known.remove(name); known.remove("arcana:$name") }
            if (!folia || owned) remove.run() else queue.add(remove)
            Unit
        }
        every { bridge.syncCommands() } answers {
            val sync = Runnable { snapshots.add(known.keys.toSet()) }
            if (!folia || owned) sync.run() else queue.add(sync)
            Unit
        }
    }
    @AfterEach fun cleanup() { unmockkAll() }
    private fun command() = ArcanaPluginCommand(mockk<PluginCommandBase>(relaxed = true), plugin, "arcana", "arcana.command", false)
    private fun tick() { owned = true; try { while (queue.isNotEmpty()) queue.remove().run() } finally { owned = false } }

    @Test fun startupRegistrationSurvivesDeferredFoliaCleanupAndSyncSeesTheNewCommand() {
        command().register(); tick()
        assertNotNull(known["arcana"], "Deferred old-command cleanup removed the newly registered root")
        assertSame(known["arcana"], known["arcana:arcana"])
        assertEquals(listOf(setOf("arcana", "arcana:arcana")), snapshots)
    }
    @Test fun registrationAndReplacementDoNotMutateTheMapBeforeTheGlobalCallback() {
        val first = command(); first.register()
        assertTrue(known.isEmpty()); tick(); val before = known["arcana"]
        command().register(); assertSame(before, known["arcana"]); tick()
        assertNotSame(before, known["arcana"]); assertNotNull(known["arcana"])
    }
    @Test fun ownedRegistrationAndRemovalStayImmediate() {
        owned = true; val command = command(); command.register(); assertNotNull(known["arcana"])
        command.unregister(); assertTrue(known.isEmpty()); assertTrue(queue.isEmpty())
    }
    @Test fun paperRegistrationKeepsItsSynchronousBehavior() {
        folia = false; command().register(); assertNotNull(known["arcana"]); assertTrue(queue.isEmpty())
    }
}
