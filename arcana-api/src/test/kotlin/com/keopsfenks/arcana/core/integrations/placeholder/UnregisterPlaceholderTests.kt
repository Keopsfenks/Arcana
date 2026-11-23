package com.keopsfenks.arcana.core.integrations.placeholder

import com.keopsfenks.arcana.core.Arcana
import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.config.base.ConfigYml
import com.keopsfenks.arcana.core.placeholder.PlayerlessPlaceholder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

internal class UnregisterPlaceholderTests {
    companion object {
        @JvmStatic
        @BeforeAll
        fun stubArcana() {
            val configYml = mockk<ConfigYml>()
            every { configYml.getInt("literal-cache-ttl") } returns 10

            val arcanaPlugin = mockk<ArcanaPlugin>()
            every { arcanaPlugin.configYml } returns configYml

            val arcana = mockk<Arcana>()
            every { arcana.arcanaPlugin } returns arcanaPlugin

            mockkStatic(Arcana::class)
            every { Arcana.get() } returns arcana
        }

        @JvmStatic
        @AfterAll
        fun unstubArcana() {
            unmockkStatic(Arcana::class)
        }
    }

    private val plugin = mockk<ArcanaPlugin>()

    @Test
    fun `unregister removes placeholder`() {
        val placeholder = PlayerlessPlaceholder(plugin, "unregister_removed") { "value" }.register()

        placeholder.unregister()

        assertNull(PlaceholderManager.getRegisteredPlaceholder(plugin, "unregister_removed"))
        assert(PlaceholderManager.getRegisteredPlaceholders(plugin).none { it === placeholder })
    }

    @Test
    fun `unregister keeps replacement`() {
        val old = PlayerlessPlaceholder(plugin, "unregister_replaced") { "old" }.register()
        val replacement = PlayerlessPlaceholder(plugin, "unregister_replaced") { "new" }.register()

        old.unregister()

        assertSame(replacement, PlaceholderManager.getRegisteredPlaceholder(plugin, "unregister_replaced"))
    }
}
