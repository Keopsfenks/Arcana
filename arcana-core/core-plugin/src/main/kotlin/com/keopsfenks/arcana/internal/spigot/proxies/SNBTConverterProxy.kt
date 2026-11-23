package com.keopsfenks.arcana.internal.spigot.proxies

import com.keopsfenks.arcana.core.items.TestableItem
import org.bukkit.inventory.ItemStack

interface SNBTConverterProxy {
    fun toSNBT(itemStack: ItemStack): String
    fun fromSNBT(snbt: String): ItemStack?
    fun makeSNBTTestable(snbt: String): TestableItem
}