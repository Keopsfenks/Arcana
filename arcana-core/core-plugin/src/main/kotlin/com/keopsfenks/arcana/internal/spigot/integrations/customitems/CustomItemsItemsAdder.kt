package com.keopsfenks.arcana.internal.spigot.integrations.customitems

import com.keopsfenks.arcana.core.integrations.customitems.CustomItemsIntegration
import com.keopsfenks.arcana.core.items.CustomItem
import com.keopsfenks.arcana.core.items.Items
import com.keopsfenks.arcana.core.items.TestableItem
import com.keopsfenks.arcana.core.items.provider.ItemProvider
import com.keopsfenks.arcana.util.namespacedKeyOf
import dev.lone.itemsadder.api.CustomStack
import java.util.function.Predicate
import org.bukkit.inventory.ItemStack

class CustomItemsItemsAdder : CustomItemsIntegration {
    override fun registerProvider() {
        Items.registerItemProvider(ItemsAdderProvider())
    }

    override fun getPluginName(): String {
        return "ItemsAdder"
    }

    private class ItemsAdderProvider : ItemProvider("itemsadder") {
        override fun provideForKey(key: String): TestableItem? {
            val internalId = if (key.contains(":")) key else "itemsadder:$key"

            val item = CustomStack.getInstance(internalId) ?: return null
            val id = item.id
            val namespacedKey = namespacedKeyOf("itemsadder", key.lowercase().replace(":", "__"))
            val stack = item.itemStack
            return CustomItem(
                namespacedKey,
                Predicate { test: ItemStack ->
                    val customStack = CustomStack.byItemStack(test) ?: return@Predicate false
                    customStack.id.equals(id, ignoreCase = true)
                },
                stack
            )
        }

        override fun getKey(itemStack: ItemStack): String? =
            CustomStack.byItemStack(itemStack)?.namespacedID
    }
}
