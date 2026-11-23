package com.keopsfenks.arcana.internal.items

import com.keopsfenks.arcana.internal.items.templates.FlagArgParser
import com.keopsfenks.arcana.util.NamespacedKeyUtils
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

object ArgParserUnconsumable : FlagArgParser("unconsumable") {
    val key = NamespacedKeyUtils.createArcanaKey("unconsumable")

    override fun apply(meta: ItemMeta) {
        meta.persistentDataContainer.set(key, PersistentDataType.BOOLEAN, true)
    }

    override fun test(meta: ItemMeta): Boolean {
        return meta.persistentDataContainer.has(key, PersistentDataType.BOOLEAN)
    }
}
