package com.keopsfenks.arcana.internal.spigot.integrations.customitems

import com.jojodmo.itembridge.ItemBridge
import com.keopsfenks.arcana.core.integrations.customitems.CustomItemsIntegration
import com.keopsfenks.arcana.core.items.CustomItem
import com.keopsfenks.arcana.core.items.Items
import com.keopsfenks.arcana.core.items.TestableItem
import com.keopsfenks.arcana.core.items.provider.ItemProvider
import com.keopsfenks.arcana.util.namespacedKeyOf
import org.bukkit.inventory.ItemStack

class CustomItemsItemBridge : CustomItemsIntegration {
    override fun registerProvider() {
        Items.registerItemProvider(ItemBridgeProvider())
    }

    override fun getPluginName(): String {
        return "ItemBridge"
    }

    private class ItemBridgeProvider : ItemProvider("itembridge") {
        override fun provideForKey(key: String): TestableItem? {

            val split = key.split(":").toMutableList()

            if (split.size < 2) {
                return null
            }

            val itemKey = split[0]

            val item = split[1]

            val stack = ItemBridge.getItemStack(itemKey, item) ?: kotlin.run {
                return null
            }

            return CustomItem(
                namespacedKeyOf("arcana:${key.lowercase().replace(":", "__")}"),
                { test: ItemStack ->
                    ItemBridge.isItemStack(test, itemKey, item)
                },
                stack
            )
        }
    }
}
