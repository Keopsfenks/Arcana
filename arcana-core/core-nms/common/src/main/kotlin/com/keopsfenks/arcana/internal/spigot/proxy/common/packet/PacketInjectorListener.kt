package com.keopsfenks.arcana.internal.spigot.proxy.common.packet

import com.keopsfenks.arcana.internal.spigot.proxy.common.toNMS
import io.netty.channel.Channel
import java.lang.reflect.Field
import net.minecraft.network.Connection
import net.minecraft.server.network.ServerCommonPacketListenerImpl
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

private const val BASE_NAME = "packet_handler"
private const val ARCANA_NAME = "arcana_packets"

// Read reflectively: a direct read of the protected field throws IllegalAccessError on Spigot.
private val connectionField: Field =
    generateSequence<Class<*>>(ServerCommonPacketListenerImpl::class.java) { it.superclass }
        .flatMap { it.declaredFields.asSequence() }
        .first { Connection::class.java.isAssignableFrom(it.type) }
        .apply { isAccessible = true }

private val Player.channel: Channel
    get() = (connectionField.get(this.toNMS().connection) as Connection).channel

object PacketInjectorListener : Listener {
    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player

        val channel = player.channel

        if (BASE_NAME !in channel.pipeline().names()) {
            return
        }

        if (ARCANA_NAME in channel.pipeline().names()) {
            return
        }

        channel.pipeline().addBefore(BASE_NAME, ARCANA_NAME, ArcanaChannelDuplexHandler(player.uniqueId))
    }

    @EventHandler
    fun onLeave(event: PlayerQuitEvent) {
        val player = event.player

        val channel = player.channel

        channel.eventLoop().submit {
            if (channel.pipeline().get(ARCANA_NAME) != null) {
                channel.pipeline().remove(ARCANA_NAME)
            }
        }
    }
}
