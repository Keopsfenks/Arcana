package com.keopsfenks.arcana.internal.spigot.proxy.v1_21_10

import com.keopsfenks.arcana.internal.spigot.proxies.TPSProxy
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.CraftServer

class TPS : TPSProxy {
    override fun getTPS(): Double {
        return (Bukkit.getServer() as CraftServer).tps[0]
    }
}
