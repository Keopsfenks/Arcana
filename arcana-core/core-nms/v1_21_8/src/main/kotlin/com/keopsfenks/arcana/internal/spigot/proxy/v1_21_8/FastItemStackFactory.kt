package com.keopsfenks.arcana.internal.spigot.proxy.v1_21_8

import com.keopsfenks.arcana.core.fast.FastItemStack
import com.keopsfenks.arcana.internal.spigot.proxies.FastItemStackFactoryProxy
import com.keopsfenks.arcana.internal.spigot.proxy.common.item.ArcanaFastItemStack
import org.bukkit.inventory.ItemStack

class FastItemStackFactory : FastItemStackFactoryProxy {
    override fun create(itemStack: ItemStack): FastItemStack {
        return ArcanaFastItemStack(itemStack)
    }
}
