package com.keopsfenks.arcana.core.skills

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.config.updating.ConfigHandler
import com.keopsfenks.arcana.core.scheduling.Scheduler
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ReloadPreflightTests {
    @Test fun `rejected preflight leaves configs and running jobs untouched`() {
        val plugin = mockk<ReloadProbePlugin>(relaxed = true)
        val configs = mockk<ConfigHandler>(relaxed = true)
        val scheduler = mockk<Scheduler>(relaxed = true)
        every { plugin.configHandler } returns configs
        every { plugin.scheduler } returns scheduler
        every { plugin.prepareReload(true) } returns false
        every { plugin.tryReload(true) } answers { callOriginal() }
        assertFalse(plugin.tryReload(true))
        verify(exactly = 0) { configs.updateConfigs() }
        verify(exactly = 0) { scheduler.cancelAll() }
    }

    @Test fun `preflight runs before config mutation and default hook accepts`() {
        val plugin = mockk<ReloadProbePlugin>(relaxed = true)
        val configs = mockk<ConfigHandler>(relaxed = true)
        val order = mutableListOf<String>()
        every { plugin.configHandler } returns configs
        every { plugin.prepareReload(true) } answers { order.add("prepare"); true }
        every { plugin.tryReload(true) } answers { callOriginal() }
        val stop = IllegalStateException("stop after observing mutation boundary")
        every { configs.updateConfigs() } answers { order.add("configs"); throw stop }
        assertSame(stop, assertThrows(IllegalStateException::class.java) { plugin.tryReload(true) })
        assertEquals(listOf("prepare", "configs"), order)
        every { plugin.prepareReload(true) } answers { callOriginal() }
        assertTrue(plugin.prepareReload(true))
    }

    @Test fun `existing void reload descriptors delegate to the boolean reload path`() {
        assertEquals(Void.TYPE, ArcanaPlugin::class.java.getMethod("reload").returnType)
        assertEquals(Void.TYPE, ArcanaPlugin::class.java.getMethod("reload", Boolean::class.javaPrimitiveType).returnType)
        val plugin = mockk<ReloadProbePlugin>(relaxed = true)
        every { plugin.reload(true) } answers { callOriginal() }
        every { plugin.tryReload(true) } returns false
        plugin.reload(true)
        verify(exactly = 1) { plugin.tryReload(true) }
    }
}
