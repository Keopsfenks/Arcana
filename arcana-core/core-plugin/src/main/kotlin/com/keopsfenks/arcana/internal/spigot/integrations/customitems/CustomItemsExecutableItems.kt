package com.keopsfenks.arcana.internal.spigot.integrations.customitems

import com.ssomar.score.api.executableitems.ExecutableItemsAPI
import com.keopsfenks.arcana.core.integrations.customitems.CustomItemsIntegration
import com.keopsfenks.arcana.core.items.CustomItem
import com.keopsfenks.arcana.core.items.Items
import com.keopsfenks.arcana.core.items.TestableItem
import com.keopsfenks.arcana.core.items.provider.ItemProvider
import com.keopsfenks.arcana.util.NamespacedKeyUtils
import java.util.Optional
import java.util.function.Predicate
import org.bukkit.inventory.ItemStack

class CustomItemsExecutableItems : CustomItemsIntegration {
    override fun registerProvider() {
        Items.registerItemProvider(ExecutableItemsProvider())
    }

    override fun getPluginName(): String {
        return "ExecutableItems"
    }

    private class ExecutableItemsProvider : ItemProvider("executableitems") {
        /*
        Item lookup strings are lowercased, but ExecutableItems IDs are
        case-sensitive, so resolve the actual ID before looking the item up.
         */
        override fun provideForKey(key: String): TestableItem? {
            val manager = ExecutableItemsAPI.getExecutableItemsManager()
            val id = manager.executableItemIdsList.firstOrNull { it.equals(key, ignoreCase = true) }
            val item = id?.let { manager.getExecutableItem(it).orElse(null) } ?: return null
            return CustomItem(
                NamespacedKeyUtils.create("executableitems", key.lowercase()),
                Predicate { test: ItemStack ->
                    val customStack = ExecutableItemsAPI.getExecutableItemsManager().getExecutableItem(test).orElse(null) ?: return@Predicate false
                    customStack.id.equals(id, ignoreCase = true)
                },
                item.buildItem(1, Optional.empty())
            )
        }

        override fun getKey(itemStack: ItemStack): String? =
            ExecutableItemsAPI.getExecutableItemsManager().getExecutableItem(itemStack).orElse(null)?.id
    }
}
