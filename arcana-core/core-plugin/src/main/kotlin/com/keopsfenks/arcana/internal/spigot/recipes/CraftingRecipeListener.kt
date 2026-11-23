package com.keopsfenks.arcana.internal.spigot.recipes

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.recipe.Recipes
import com.keopsfenks.arcana.util.namespacedKeyOf
import org.bukkit.Keyed
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.player.PlayerRecipeDiscoverEvent

class CraftingRecipeListener(val plugin: ArcanaPlugin) : Listener {
    @EventHandler
    fun handleDisplayedRecipeUnlocks(event: PlayerRecipeDiscoverEvent) {
        if (!ArcanaPlugin.getPluginNames().contains(event.recipe.namespace)) {
            return
        }

        if (!event.recipe.key.contains("_displayed")) {
            event.isCancelled = true

            val player = event.player
            player.discoverRecipe(
                namespacedKeyOf(
                    event.recipe.namespace,
                    event.recipe.key + "_displayed"
                )
            )
        }
    }

    @EventHandler
    fun processListeners(event: PrepareItemCraftEvent) {
        handlePrepare(event)

        if (plugin.configYml.getBool("enforce-preparing-recipes")) {
            val player = event.viewers.getOrNull(0) as? Player ?: return
            plugin.scheduler.on(player).runLater(1) {
                handlePrepare(event)
            }
        }
    }

    private fun handlePrepare(event: PrepareItemCraftEvent) {
        var recipe = event.recipe as? Keyed

        if (recipe == null) {
            val arcanaRecipe = Recipes.getMatch(event.inventory.matrix)
            if (arcanaRecipe != null) {
                recipe = Keyed { arcanaRecipe.key }
            }
        }

        if (recipe == null) {
            return
        }

        for (listener in listeners) {
            listener.handle(WrappedPrepareItemCraftEvent(event, recipe))
        }
    }

    @EventHandler
    fun processListeners(event: CraftItemEvent) {
        if (event.recipe !is Keyed) {
            return
        }

        for (listener in listeners) {
            listener.handle(WrappedCraftItemEvent(event))
        }
    }

    companion object {
        val validators = mutableListOf<RecipeValidator>()
        private val listeners = mutableListOf<RecipeListener>()

        fun registerValidator(validator: RecipeValidator) {
            validators.add(validator)
        }

        fun registerListener(listener: RecipeListener) {
            listeners.add(listener)
        }
    }
}
