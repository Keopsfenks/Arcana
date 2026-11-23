package com.keopsfenks.arcana.skills.triggers

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.skills.SkillContext
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerToggleSneakEvent

class SneakTrigger(plugin: ArcanaPlugin) : BukkitSkillTrigger("sneak", plugin) {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEvent(event: PlayerToggleSneakEvent) {
        if (!event.isCancelled && event.isSneaking) emit(SkillContext.builder(event.player).build())
    }
}
