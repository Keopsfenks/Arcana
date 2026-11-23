package com.keopsfenks.arcana.internal.spigot.proxies

import com.keopsfenks.arcana.core.ArcanaPlugin
import org.bukkit.NamespacedKey
import org.bukkit.inventory.Recipe

interface CommonsInitializerProxy {
    fun init(plugin: ArcanaPlugin)

    fun addBukkitRecipeNoResend(recipe: Recipe)

    fun reloadBukkitRecipes()

    fun removeBukkitRecipeNoResend(key: NamespacedKey): Boolean
}
