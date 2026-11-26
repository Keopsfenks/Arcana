package com.keopsfenks.arcana.skills.triggers

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.skills.SkillContext
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

class LeftClickTrigger(plugin: ArcanaPlugin) : BukkitSkillTrigger("left_click", plugin) {
    @Suppress("DEPRECATION")
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEvent(event: PlayerInteractEvent) {
        if (event.isCancelled || event.hand != EquipmentSlot.HAND || event.action !in setOf(Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK)) return
        val context = SkillContext.builder(event.player)
        event.clickedBlock?.let { context.value("block", it.type.name) }
        emit(context.build())
    }
}
