package com.keopsfenks.arcana.internal.events

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.events.EventManager
import com.keopsfenks.arcana.core.map.listMap
import com.keopsfenks.arcana.core.packet.PacketEvent
import com.keopsfenks.arcana.core.packet.PacketListener
import com.keopsfenks.arcana.core.packet.PacketPriority
import org.bukkit.Bukkit
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener


private class RegisteredPacketListener(
    val plugin: ArcanaPlugin,
    val listener: PacketListener
)

private val listeners = listMap<PacketPriority, RegisteredPacketListener>()

fun PacketEvent.handleSend() {
    for (priority in PacketPriority.entries) {
        for (listener in listeners[priority]) {
            try {
                listener.listener.onSend(this)
            } catch (e: Exception) {
                listener.plugin.logger.warning(
                    "Exception in packet listener ${listener.listener.javaClass.name}" +
                            " for packet ${packet.handle.javaClass.name}!"
                )
                e.printStackTrace()
            } catch (e: LinkageError) {
                listener.plugin.logger.warning(
                    "Error in packet listener ${listener.listener.javaClass.name}" +
                            " for packet ${packet.handle.javaClass.name}!"
                )
                e.printStackTrace()
            }
        }
    }
}

fun PacketEvent.handleReceive() {
    for (priority in PacketPriority.entries) {
        for (listener in listeners[priority]) {
            try {
                listener.listener.onReceive(this)
            } catch (e: Exception) {
                listener.plugin.logger.warning(
                    "Exception in packet listener ${listener.listener.javaClass.name}" +
                            " for packet ${packet.handle.javaClass.name}!"
                )
                e.printStackTrace()
            } catch (e: LinkageError) {
                listener.plugin.logger.warning(
                    "Error in packet listener ${listener.listener.javaClass.name}" +
                            " for packet ${packet.handle.javaClass.name}!"
                )
                e.printStackTrace()
            }
        }
    }
}

class ArcanaEventManager(private val plugin: ArcanaPlugin) : EventManager {
    override fun registerListener(listener: Listener) {
        Bukkit.getPluginManager().registerEvents(listener, plugin)
    }

    override fun unregisterListener(listener: Listener) {
        HandlerList.unregisterAll(listener)
    }

    override fun unregisterAllListeners() {
        HandlerList.unregisterAll(plugin)
        for (value in listeners.values) {
            value.removeIf { it.plugin == plugin }
        }
    }

    override fun registerPacketListener(listener: PacketListener) {
        listeners[listener.priority].add(
            RegisteredPacketListener(
                plugin,
                listener
            )
        )
    }
}
