package com.keopsfenks.arcana.skills.triggers

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.skills.SkillContext
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent

class JoinTrigger(plugin: ArcanaPlugin) : BukkitSkillTrigger("join", plugin) {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onEvent(event: PlayerJoinEvent) { emit(SkillContext.builder(event.player).build()) }
}
