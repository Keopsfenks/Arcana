package com.keopsfenks.arcana.internal.factory

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.factory.NamespacedKeyFactory
import org.bukkit.NamespacedKey

class ArcanaNamespacedKeyFactory(private val plugin: ArcanaPlugin) : NamespacedKeyFactory {
    override fun create(key: String): NamespacedKey {
        return NamespacedKey(plugin.id, key)
    }
}
