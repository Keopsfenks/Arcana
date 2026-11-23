package com.keopsfenks.arcana.internal.spigot.integrations.customitems

import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.api.events.NexoItemsLoadedEvent
import com.nexomc.nexo.items.UpdateCallback
import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.integrations.customitems.CustomItemsIntegration
import com.keopsfenks.arcana.core.items.CustomItem
import com.keopsfenks.arcana.core.items.Items
import com.keopsfenks.arcana.core.items.TestableItem
import com.keopsfenks.arcana.core.items.provider.ItemProvider
import com.keopsfenks.arcana.util.namespacedKeyOf
import net.kyori.adventure.key.Key
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack

class CustomItemsNexo(
    private val plugin: ArcanaPlugin
) : CustomItemsIntegration, Listener {
    override fun registerProvider() {
        plugin.eventManager.registerListener(this)
    }

    override fun getPluginName(): String {
        return "Nexo"
    }

    @EventHandler
    @Suppress("UNUSED_PARAMETER")
    fun onItemRegister(event: NexoItemsLoadedEvent) {
        Items.registerItemProvider(NexoProvider())

        NexoItems.registerUpdateCallback(
            Key.key("arcana:nexo_update"),
            object : UpdateCallback {
                override fun preUpdate(itemStack: ItemStack?): ItemStack? {
                    val arcanaArmorNamespacedKey = namespacedKeyOf("ecoarmor", "set")
                    val arcanaItemsNamespacedKey = namespacedKeyOf("ecoitems", "item")
                    val keys = itemStack?.persistentDataContainer?.keys ?: return super.preUpdate(itemStack)

                    if (
                        keys.contains(arcanaArmorNamespacedKey)
                        || keys.contains(arcanaItemsNamespacedKey)
                    ) {
                        return null
                    }

                    return super.preUpdate(itemStack)
                }
            }
        )
    }

    private class NexoProvider : ItemProvider("nexo") {
        override fun provideForKey(key: String): TestableItem? {
            val item = NexoItems.itemFromId(key) ?: return null
            val id = NexoItems.idFromItem(item)
            val namespacedKey = namespacedKeyOf("nexo", id.toString())
            return CustomItem(
                namespacedKey,
                { id.equals(NexoItems.idFromItem(it), ignoreCase = true) },
                item.build()
            )
        }

        override fun getKey(itemStack: ItemStack): String? =
            NexoItems.idFromItem(itemStack)
    }
}