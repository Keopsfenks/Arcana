package com.keopsfenks.arcana.skills.effects

import org.bukkit.NamespacedKey
import org.bukkit.potion.PotionEffectType
import java.util.Locale

internal object PotionValues {
    @Suppress("DEPRECATION")
    fun resolve(name: String): PotionEffectType? {
        val normalized = name.lowercase(Locale.ROOT)
        val key = NamespacedKey.fromString(if (':' in normalized) normalized else "minecraft:$normalized") ?: return null
        return PotionEffectType.getByKey(key)
    }
}
