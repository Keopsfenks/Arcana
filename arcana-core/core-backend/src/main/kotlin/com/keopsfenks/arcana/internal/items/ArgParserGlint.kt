package com.keopsfenks.arcana.internal.items

import com.keopsfenks.arcana.internal.items.templates.FlagArgParser
import org.bukkit.inventory.meta.ItemMeta

object ArgParserGlint : FlagArgParser("glint") {
    override fun apply(meta: ItemMeta) {
        meta.setEnchantmentGlintOverride(true)
    }

    override fun test(meta: ItemMeta): Boolean {
        return meta.hasEnchantmentGlintOverride()
    }
}
