package com.keopsfenks.arcana.internal.items

import com.keopsfenks.arcana.internal.items.templates.FlagArgParser
import org.bukkit.inventory.meta.ItemMeta

object ArgParserGlider : FlagArgParser("glider") {
    override fun apply(meta: ItemMeta) {
        meta.isGlider = true
    }

    override fun test(meta: ItemMeta): Boolean {
        return meta.isGlider
    }
}
