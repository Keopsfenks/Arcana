package com.keopsfenks.arcana.internal.spigot.proxy.v1_21_8

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.packet.PacketListener
import com.keopsfenks.arcana.internal.spigot.ArcanaSpigotPlugin
import com.keopsfenks.arcana.internal.spigot.proxies.PacketHandlerProxy
import com.keopsfenks.arcana.internal.spigot.proxy.common.packet.PacketActionBarDetection
import com.keopsfenks.arcana.internal.spigot.proxy.common.packet.PacketSeenAdvancements
import com.keopsfenks.arcana.internal.spigot.proxy.common.packet.display.PacketHeldItemSlot
import com.keopsfenks.arcana.internal.spigot.proxy.common.packet.display.PacketRenameItem
import com.keopsfenks.arcana.internal.spigot.proxy.common.packet.display.PacketSetEntityData
import com.keopsfenks.arcana.internal.spigot.proxy.common.packet.display.PacketSetSlot
import com.keopsfenks.arcana.internal.spigot.proxy.common.packet.display.frame.clearDisplayFrame
import com.keopsfenks.arcana.internal.spigot.proxy.common.packet.display.frame.clearFrames
import com.keopsfenks.arcana.internal.spigot.proxy.v1_21_8.packet.NewItemsPacketOpenWindowMerchant
import com.keopsfenks.arcana.internal.spigot.proxy.v1_21_8.packet.NewItemsPacketSetCreativeSlot
import com.keopsfenks.arcana.internal.spigot.proxy.v1_21_8.packet.NewItemsPacketWindowItems
import com.keopsfenks.arcana.internal.spigot.proxy.v1_21_8.packet.PacketContainerClick
import com.keopsfenks.arcana.internal.spigot.proxy.v1_21_8.packet.PacketSetCursorItem
import net.minecraft.network.protocol.Packet
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player

class PacketHandler : PacketHandlerProxy {
    override fun sendPacket(player: Player, packet: com.keopsfenks.arcana.core.packet.Packet) {
        if (player !is CraftPlayer) {
            return
        }

        val handle = packet.handle

        if (handle !is Packet<*>) {
            return
        }

        player.handle.connection.send(handle)
    }

    override fun clearDisplayFrames() {
        clearFrames()
    }

    override fun clearDisplayFrame(player: Player) {
        player.clearDisplayFrame()
    }

    override fun getPacketListeners(plugin: ArcanaPlugin): List<PacketListener> {
        return listOf(
            PacketHeldItemSlot,
            PacketRenameItem,
            NewItemsPacketOpenWindowMerchant,
            NewItemsPacketSetCreativeSlot,
            PacketSetSlot,
            PacketSetEntityData,
            NewItemsPacketWindowItems(plugin),
            PacketContainerClick,
            PacketSetCursorItem,
            PacketActionBarDetection((plugin as ArcanaSpigotPlugin).persistentActionBars),
            PacketSeenAdvancements
        )
    }
}
