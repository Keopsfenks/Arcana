package com.keopsfenks.arcana.internal.fast

import org.bukkit.NamespacedKey

class SafeInternalNamespacedKeyFactory {
    fun create(namespace: String, key: String): NamespacedKey {
        return NamespacedKey(namespace, key)
    }
}
