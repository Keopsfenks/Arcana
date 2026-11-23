@file:JvmName("PacketExtensions")

package com.keopsfenks.arcana.core.packet

import org.bukkit.entity.Player

/** @see Packet.send */
fun Player.sendPacket(packet: Packet) =
    packet.send(this)
