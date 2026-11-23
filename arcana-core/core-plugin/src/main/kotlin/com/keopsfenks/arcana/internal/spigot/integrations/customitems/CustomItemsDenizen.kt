package com.keopsfenks.arcana.internal.spigot.integrations.customitems

import com.denizenscript.denizen.objects.ItemTag
import com.denizenscript.denizen.scripts.containers.core.ItemScriptHelper
import com.keopsfenks.arcana.core.integrations.customitems.CustomItemsIntegration
import com.keopsfenks.arcana.core.items.CustomItem
import com.keopsfenks.arcana.core.items.Items
import com.keopsfenks.arcana.core.items.TestableItem
import com.keopsfenks.arcana.core.items.provider.ItemProvider
import com.keopsfenks.arcana.util.NamespacedKeyUtils
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack

class CustomItemsDenizen : CustomItemsIntegration, Listener {

    override fun registerProvider() {
        Items.registerItemProvider(DenizenProvider())
    }

    override fun getPluginName(): String {
        return "Denizen"
    }

    private class DenizenProvider : ItemProvider("denizen") {
        override fun provideForKey(key: String): TestableItem? {
            val item = ItemTag.valueOf(key, false) ?: return null
            val id = item.scriptName
            val namespacedKey = NamespacedKeyUtils.create("denizen", id)
            val stack = item.itemStack
            return CustomItem(
                namespacedKey,
                { id.equals(ItemScriptHelper.getItemScriptNameText(it), ignoreCase = true) },
                stack
            )
        }

        override fun getKey(itemStack: ItemStack): String? =
            ItemScriptHelper.getItemScriptNameText(itemStack)
    }
}
