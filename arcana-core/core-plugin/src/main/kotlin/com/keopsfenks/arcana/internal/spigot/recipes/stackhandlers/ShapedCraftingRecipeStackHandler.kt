package com.keopsfenks.arcana.internal.spigot.recipes.stackhandlers

import com.keopsfenks.arcana.core.items.TestableItem
import com.keopsfenks.arcana.core.recipe.recipes.CraftingRecipe
import com.keopsfenks.arcana.core.recipe.recipes.ShapedCraftingRecipe
import com.keopsfenks.arcana.internal.spigot.recipes.StackedRecipeHandler
import org.bukkit.inventory.ItemStack

object ShapedCraftingRecipeStackHandler : StackedRecipeHandler {
    override val recipeType = ShapedCraftingRecipe::class.java
    override fun makeData(recipe: CraftingRecipe): Any? = null

    override fun getPart(recipe: CraftingRecipe, position: Int, item: ItemStack, data: Any?): TestableItem? =
        recipe.parts[position]
}