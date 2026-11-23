package com.keopsfenks.arcana.internal.spigot.proxies

import com.keopsfenks.arcana.core.fast.FastItemStack
import org.bukkit.inventory.ItemStack

interface FastItemStackFactoryProxy {
    fun create(itemStack: ItemStack): FastItemStack
}