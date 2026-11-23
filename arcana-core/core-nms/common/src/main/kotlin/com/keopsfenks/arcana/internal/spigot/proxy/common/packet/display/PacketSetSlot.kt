package com.keopsfenks.arcana.internal.spigot.proxy.common.packet.display

import com.keopsfenks.arcana.core.display.Display
import com.keopsfenks.arcana.core.packet.PacketEvent
import com.keopsfenks.arcana.core.packet.PacketListener
import com.keopsfenks.arcana.internal.spigot.proxy.common.asBukkitStack
import com.keopsfenks.arcana.internal.spigot.proxy.common.packet.display.frame.DisplayFrame
import com.keopsfenks.arcana.internal.spigot.proxy.common.packet.display.frame.lastDisplayFrame
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket

object PacketSetSlot : PacketListener {
    override fun onSend(event: PacketEvent) {
        val packet = event.packet.handle as? ClientboundContainerSetSlotPacket ?: return

        Display.display(packet.item.asBukkitStack(), event.player)

        event.player.lastDisplayFrame = DisplayFrame.EMPTY
    }
}
