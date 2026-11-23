package com.keopsfenks.arcana.internal.spigot.proxies

import com.keopsfenks.arcana.core.packet.Packet
import org.bukkit.entity.Player

interface WorkstationPacketProxy {
    fun getContainerClickSlot(packet: Packet): Int?

    fun getOpenContainerId(player: Player): Int

    fun sendContainerDataPacket(player: Player, containerId: Int, value: Int)
}
