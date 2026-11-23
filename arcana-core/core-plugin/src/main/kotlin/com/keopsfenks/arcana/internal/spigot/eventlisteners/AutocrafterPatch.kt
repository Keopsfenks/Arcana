@file:Suppress("UnstableApiUsage")

package com.keopsfenks.arcana.internal.spigot.eventlisteners

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.recipe.Recipes
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.CrafterCraftEvent

object AutocrafterPatch : Listener {
    @EventHandler
    fun preventArcanaRecipes(event: CrafterCraftEvent) {
        val key = event.recipe.key
        if (!ArcanaPlugin.getPluginNames().contains(key.namespace)) {
            return
        }

        // Strip the "_crafter" suffix used by support-crafter Bukkit recipes
        // so we can look up the underlying arcana CraftingRecipe.
        val lookupKey = if (key.key.endsWith("_crafter")) {
            NamespacedKey(key.namespace, key.key.removeSuffix("_crafter"))
        } else {
            key
        }
        val recipe = Recipes.getRecipe(lookupKey)
        if (recipe != null && recipe.isCrafterSupported) {
            return
        }

        event.isCancelled = true
    }
}
