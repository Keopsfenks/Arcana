package com.keopsfenks.arcana.internal.spigot.integrations.customitems

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.integrations.customitems.CustomItemsIntegration
import com.keopsfenks.arcana.core.items.CustomItem
import com.keopsfenks.arcana.core.items.Items
import com.keopsfenks.arcana.core.items.TestableItem
import com.keopsfenks.arcana.core.items.provider.ItemProvider
import com.keopsfenks.arcana.util.namespacedKeyOf
import io.th0rgal.oraxen.api.OraxenItems
import io.th0rgal.oraxen.api.events.OraxenItemsLoadedEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack

class CustomItemsOraxen(
    private val plugin: ArcanaPlugin
) : CustomItemsIntegration, Listener {
    override fun registerProvider() {
        plugin.eventManager.registerListener(this)
    }

    override fun getPluginName(): String {
        return "Oraxen"
    }

    @EventHandler
    @Suppress("UNUSED_PARAMETER")
    fun onItemRegister(event: OraxenItemsLoadedEvent) {
        Items.registerItemProvider(OraxenProvider())
    }

    private class OraxenProvider : ItemProvider("oraxen") {
        override fun provideForKey(key: String): TestableItem? {
            val item = OraxenItems.getItemById(key) ?: return null
            val id = OraxenItems.getIdByItem(item)
            val namespacedKey = namespacedKeyOf("oraxen", id)
            return CustomItem(
                namespacedKey,
                { id.equals(OraxenItems.getIdByItem(it), ignoreCase = true) },
                item.build()
            )
        }

        override fun getKey(itemStack: ItemStack): String? =
            OraxenItems.getIdByItem(itemStack)
    }
}
