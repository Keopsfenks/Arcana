package com.keopsfenks.arcana.core.skills

import com.keopsfenks.arcana.core.*
import com.keopsfenks.arcana.core.command.impl.PluginCommand
import com.keopsfenks.arcana.core.scheduling.TaskContext
import io.mockk.*
import org.bukkit.Bukkit
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class CommandBatchOwnershipTests {
    private val plugin = mockk<ReloadProbePlugin>(relaxed = true)
    private val bridge = mockk<Arcana>(relaxed = true)
    private val global = mockk<TaskContext>()
    private val pending = mutableListOf<Runnable>()
    private val events = mutableListOf<String>()
    private var owned = false
    private var folia = true
    @BeforeEach fun setup() {
        mockkStatic(Arcana::class, Bukkit::class); mockkObject(Prerequisite.HAS_FOLIA)
        every { Arcana.get() } returns bridge
        every { Prerequisite.HAS_FOLIA.isMet } answers { folia }
        every { plugin.scheduler.global() } returns global
        every { global.run(any<Runnable>()) } answers { pending.add(firstArg()); mockk(relaxed = true) }
        every { bridge.beginCommandBatch() } answers { check(!folia || owned); events.add("begin") }
        every { bridge.endCommandBatch() } answers { check(!folia || owned); events.add("end") }
        val commands = listOf("first", "second").map { name -> mockk<PluginCommand>().also { command -> every { command.register() } answers { check(!folia || owned); events.add(name) } } }
        every { plugin.loadPluginCommands() } returns commands
        every { plugin["registerPluginCommandsOnOwner"]() } answers { callOriginal() }
    }
    @AfterEach fun cleanup() { unmockkAll() }
    private fun registerBatch() { ArcanaPlugin::class.java.getDeclaredMethod("registerPluginCommandsOnOwner").apply { isAccessible = true }.invoke(plugin) }
    @Test fun entireStartupBatchMovesTogetherToTheGlobalOwner() {
        registerBatch(); assertTrue(events.isEmpty()); assertEquals(1, pending.size)
        owned = true; pending.single().run(); assertEquals(listOf("begin", "first", "second", "end"), events)
    }
    @Test fun foliaBatchStillUsesOneGlobalTaskWhenEnableAlreadyHasAnOwner() {
        owned = true; registerBatch(); assertTrue(events.isEmpty()); assertEquals(1, pending.size)
        pending.single().run(); assertEquals(listOf("begin", "first", "second", "end"), events)
    }
    @Test fun paperBatchRemainsSynchronous() {
        folia = false; registerBatch(); assertEquals(listOf("begin", "first", "second", "end"), events); assertTrue(pending.isEmpty())
    }
}
