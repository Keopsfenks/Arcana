package com.keopsfenks.arcana.internal.spigot.proxy.common.packet.display

import com.keopsfenks.arcana.core.packet.PacketEvent
import com.keopsfenks.arcana.core.packet.PacketListener
import com.keopsfenks.arcana.internal.spigot.proxy.common.packet.display.frame.DisplayFrame
import com.keopsfenks.arcana.internal.spigot.proxy.common.packet.display.frame.lastDisplayFrame
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket

object PacketHeldItemSlot : PacketListener {
    override fun onReceive(event: PacketEvent) {
        if (event.packet.handle !is ServerboundSetCarriedItemPacket) {
            return
        }

        event.player.lastDisplayFrame = DisplayFrame.EMPTY
    }
}
