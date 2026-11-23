package com.keopsfenks.arcana.internal.spigot.data.handlers

import com.keopsfenks.arcana.core.Arcana
import com.keopsfenks.arcana.core.config.ConfigType
import com.keopsfenks.arcana.core.config.interfaces.Config
import com.keopsfenks.arcana.internal.config.ArcanaConfigSection
import com.keopsfenks.arcana.internal.config.toMap
import com.keopsfenks.arcana.internal.spigot.data.KeyRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.bukkit.NamespacedKey

/**
 * The CONFIG key type serializes through [com.keopsfenks.arcana.core.config.readConfig], which reaches the
 * arcana singleton, so a config round trip cannot be tested against a relaxed mock -- it has to answer
 * with a real config implementation.
 */
fun stubArcanaConfigFactory() {
    val arcana = mockk<Arcana>(relaxed = true)

    every { arcana.createConfig(any<String>(), any<ConfigType>()) } answers {
        val type = secondArg<ConfigType>()
        ArcanaConfigSection(type, type.toMap(firstArg<String>()))
    }

    every { arcana.createConfig(any<Map<String, Any>>(), any<ConfigType>()) } answers {
        ArcanaConfigSection(secondArg(), firstArg<Map<String, Any>>())
    }

    // A value's stored path and column are built from the key's string form, so a mocked key
    // would read and write at a path no fixture can be written against.
    every { arcana.createNamespacedKey(any(), any()) } answers {
        NamespacedKey(firstArg<String>(), secondArg<String>())
    }

    // getSavedUUIDs decides which tables to scan from the key registry, which is where the real
    // implementation routes key registration.
    every { arcana.registerPersistentKey(any()) } answers { KeyRegistry.registerKey(firstArg()) }

    mockkStatic(Arcana::class)
    every { Arcana.get() } returns arcana
}

fun unstubArcanaConfigFactory() {
    unmockkStatic(Arcana::class)
}

fun configOf(vararg values: Pair<String, Any?>): Config =
    ArcanaConfigSection(ConfigType.YAML, mapOf(*values))
