package com.keopsfenks.arcana.internal.spigot.integrations.multiverseinventories

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.events.ArmorChangeEvent
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.mvplugins.multiverse.inventories.event.WorldChangeShareHandlingEvent

class MultiverseInventoriesIntegration(
    private val plugin: ArcanaPlugin
): Listener {
    @EventHandler
    fun onWorldChange(event: WorldChangeShareHandlingEvent) {
        val before = event.player.inventory.armorContents.toMutableList()
        this.plugin.scheduler.on(event.player).run {
            val after = event.player.inventory.armorContents.toMutableList()
            Bukkit.getPluginManager().callEvent(ArmorChangeEvent(event.player, before, after))
        }
    }
}