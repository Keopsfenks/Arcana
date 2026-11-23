package com.keopsfenks.arcana.internal.spigot.recipes.listeners

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.recipe.Recipes
import com.keopsfenks.arcana.internal.spigot.recipes.CraftingRecipeListener
import com.keopsfenks.arcana.internal.spigot.recipes.GenericCraftEvent
import com.keopsfenks.arcana.internal.spigot.recipes.RecipeListener
import org.bukkit.entity.Player

object ComplexInComplex : RecipeListener {
    override fun handle(event: GenericCraftEvent) {
        val recipe = event.recipe

        if (!ArcanaPlugin.getPluginNames().contains(recipe.key.namespace)) {
            return
        }

        if (event.inventory.size == 5) {
            event.deny()
            return
        }

        val player = event.inventory.viewers.getOrNull(0) as? Player ?: return

        val matrix = event.inventory.matrix

        if (CraftingRecipeListener.validators.any { it.validate(event) }) {
            return
        }

        val matched = Recipes.getMatch(matrix)

        if (matched == null) {
            event.deny()
            return
        }

        if (matched.test(matrix)) {
            if (matched.permission != null) {
                if (player.hasPermission(matched.permission!!)) {
                    event.allow(matched)
                } else {
                    event.deny()
                }
            } else {
                event.allow(matched)
            }
        } else {
            event.deny()
        }
    }
}
