package com.keopsfenks.arcana.skills.triggers

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.skills.SkillContext
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerToggleSprintEvent

class SprintTrigger(plugin: ArcanaPlugin) : BukkitSkillTrigger("sprint", plugin) {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEvent(event: PlayerToggleSprintEvent) {
        if (!event.isCancelled && event.isSprinting) emit(SkillContext.builder(event.player).build())
    }
}
