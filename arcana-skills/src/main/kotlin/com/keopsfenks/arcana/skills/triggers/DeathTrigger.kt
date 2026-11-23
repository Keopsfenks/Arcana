package com.keopsfenks.arcana.skills.triggers

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.skills.SkillContext
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.PlayerDeathEvent

class DeathTrigger(plugin: ArcanaPlugin) : BukkitSkillTrigger("death", plugin) {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEvent(event: PlayerDeathEvent) {
        if (!cancelled(event)) emit(SkillContext.builder(event.entity).build())
    }
}
