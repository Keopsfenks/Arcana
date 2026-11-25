package com.keopsfenks.arcana.skills.triggers

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.skills.SkillContext
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.PlayerDeathEvent

class DeathTrigger(plugin: ArcanaPlugin) : BukkitSkillTrigger("death", plugin) {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEvent(event: PlayerDeathEvent) {
        if (com.keopsfenks.arcana.skills.execution.SkillDamageScope.isActive() || cancelled(event)) return
        val builder = SkillContext.builder(event.entity)
        event.damageSource.directEntity?.let { builder.value("damage_source_id", it.uniqueId.toString()) }
        emit(builder.build())
    }
}
