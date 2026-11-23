package com.keopsfenks.arcana.internal.spigot.proxies

import org.bukkit.entity.Player

interface PlayerHandlerProxy {
    fun giveExpAndApplyMending(player: Player, amount: Int, applyMending: Boolean)

    fun Player.applyMending(amount: Int) : Int
}