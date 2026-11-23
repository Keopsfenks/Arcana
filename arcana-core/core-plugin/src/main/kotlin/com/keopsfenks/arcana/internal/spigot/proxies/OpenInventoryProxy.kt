package com.keopsfenks.arcana.internal.spigot.proxies

import org.bukkit.entity.Player

interface OpenInventoryProxy {
    /** Returns the NMS container menu the player currently has open. */
    fun getOpenInventory(player: Player): Any
}
