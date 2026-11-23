package com.keopsfenks.arcana.skills.triggers

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.skills.SkillContext
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerRespawnEvent

class RespawnTrigger(plugin: ArcanaPlugin) : BukkitSkillTrigger("respawn", plugin) {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onEvent(event: PlayerRespawnEvent) { emit(SkillContext.builder(event.player).build()) }
}
