package com.keopsfenks.arcana.skills.triggers

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.skills.SkillContext
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.ProjectileHitEvent

class ProjectileHitTrigger(plugin: ArcanaPlugin) : BukkitSkillTrigger("projectile_hit", plugin) {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEvent(event: ProjectileHitEvent) {
        if (event.isCancelled) return
        val player = event.entity.shooter as? Player ?: return
        val victim = event.hitEntity
        val context = SkillContext.builder(player).victim(victim as? LivingEntity)
        victim?.let { context.value("entity", it.type.name) }
        event.hitBlock?.let { context.value("block", it.type.name) }
        emit(context.build())
    }
}
