package com.keopsfenks.arcana.internal.spigot.proxy.v1_21_8.packet

import com.keopsfenks.arcana.core.packet.PacketEvent
import com.keopsfenks.arcana.core.packet.PacketListener
import com.keopsfenks.arcana.internal.spigot.proxy.common.packet.display.CursorOriginals
import com.keopsfenks.arcana.internal.spigot.proxy.common.packet.display.frame.DisplayFrame
import com.keopsfenks.arcana.internal.spigot.proxy.common.packet.display.frame.lastDisplayFrame
import net.minecraft.network.HashedStack
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket

object PacketContainerClick : PacketListener {
    override fun onReceive(event: PacketEvent) {
        val packet = event.packet.handle as? ServerboundContainerClickPacket ?: return

        val carried = packet.carriedItem as? HashedStack.ActualItem ?: return
        val player = event.player
        val original = CursorOriginals.get(player.uniqueId, carried.hash()) ?: return

        event.packet.handle = ServerboundContainerClickPacket(
            packet.containerId,
            packet.stateId,
            packet.slotNum,
            packet.buttonNum,
            packet.clickType,
            packet.changedSlots,
            original
        )

        player.lastDisplayFrame = DisplayFrame.EMPTY
    }
}
