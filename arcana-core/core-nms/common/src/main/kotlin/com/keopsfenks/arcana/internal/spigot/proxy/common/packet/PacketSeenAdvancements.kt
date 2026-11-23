package com.keopsfenks.arcana.internal.spigot.proxy.common.packet

import com.github.benmanes.caffeine.cache.Caffeine
import com.keopsfenks.arcana.core.events.PlayerOpenAdvancementsEvent
import com.keopsfenks.arcana.core.packet.PacketEvent
import com.keopsfenks.arcana.core.packet.PacketListener
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object PacketSeenAdvancements : PacketListener {
    private val openScreens = Caffeine.newBuilder()
        .weakKeys()
        .build<Player, Boolean>()
        .asMap()

    override fun onReceive(event: PacketEvent) {
        val packet = event.packet.handle as? ServerboundSeenAdvancementsPacket ?: return
        val player = event.player

        when (packet.action) {
            ServerboundSeenAdvancementsPacket.Action.OPENED_TAB -> {
                if (openScreens.putIfAbsent(player, true) != null) {
                    return
                }

                if (PlayerOpenAdvancementsEvent.getHandlerList().registeredListeners.isEmpty()) {
                    return
                }

                Bukkit.getPluginManager().callEvent(PlayerOpenAdvancementsEvent(player))
            }

            ServerboundSeenAdvancementsPacket.Action.CLOSED_SCREEN -> openScreens.remove(player)
        }
    }
}
