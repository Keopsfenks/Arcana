package com.keopsfenks.arcana.internal.spigot.data

import com.keopsfenks.arcana.core.data.keys.PersistentDataKey
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.NamespacedKey

object KeyRegistry {
    // Concurrent because cross-server sync looks keys up from its own thread while plugins may
    // still be registering theirs.
    private val registry = ConcurrentHashMap<NamespacedKey, PersistentDataKey<*>>()

    fun registerKey(key: PersistentDataKey<*>) {
        if (key.defaultValue == null) {
            throw IllegalArgumentException("Default value cannot be null!")
        }

        this.registry[key.key] = key
    }

    fun getRegisteredKeys(): Set<PersistentDataKey<*>> {
        return registry.values.toSet()
    }

    fun getKey(key: NamespacedKey): PersistentDataKey<*>? {
        return registry[key]
    }
}
