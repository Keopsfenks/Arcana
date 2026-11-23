package com.keopsfenks.arcana.internal.spigot.gui

import com.keopsfenks.arcana.internal.gui.menu.removeForcedRenderedInventory
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

object GUIQuitListener : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    fun handleQuit(event: PlayerQuitEvent) {
        event.player.removeForcedRenderedInventory()
    }
}
