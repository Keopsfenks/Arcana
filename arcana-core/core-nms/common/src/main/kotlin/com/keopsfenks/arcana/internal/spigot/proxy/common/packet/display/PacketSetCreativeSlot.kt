package com.keopsfenks.arcana.internal.spigot.proxy.common.packet.display

import com.keopsfenks.arcana.core.display.Display
import com.keopsfenks.arcana.core.packet.PacketEvent
import com.keopsfenks.arcana.core.packet.PacketListener
import com.keopsfenks.arcana.internal.spigot.proxy.common.asBukkitStack
import com.keopsfenks.arcana.internal.spigot.proxy.common.packet.display.frame.DisplayFrame
import com.keopsfenks.arcana.internal.spigot.proxy.common.packet.display.frame.lastDisplayFrame
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket

object PacketSetCreativeSlot : PacketListener {
    override fun onReceive(event: PacketEvent) {
        val packet = event.packet.handle as? ServerboundSetCreativeModeSlotPacket ?: return

        Display.revert(packet.itemStack.asBukkitStack())

        event.player.lastDisplayFrame = DisplayFrame.EMPTY
    }
}