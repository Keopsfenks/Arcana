package com.keopsfenks.arcana.internal.spigot.proxies

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.packet.Packet
import com.keopsfenks.arcana.core.packet.PacketListener
import org.bukkit.entity.Player

interface PacketHandlerProxy {
    fun sendPacket(player: Player, packet: Packet)

    fun clearDisplayFrames()

    fun clearDisplayFrame(player: Player)

    fun getPacketListeners(plugin: ArcanaPlugin): List<PacketListener>
}
