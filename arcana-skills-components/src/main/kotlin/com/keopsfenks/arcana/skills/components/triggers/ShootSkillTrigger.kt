package com.keopsfenks.arcana.skills.components.triggers
import com.keopsfenks.arcana.core.ArcanaPlugin
import org.bukkit.event.*
import org.bukkit.event.entity.EntityShootBowEvent
class ShootSkillTrigger(plugin: ArcanaPlugin) : BukkitPlatformTrigger("shoot", plugin, setOf("hand", "force", "material", "projectile_id")) {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEvent(event: EntityShootBowEvent) { if (!event.isCancelled) emit(event.entity, values = mapOf("hand" to event.hand.name, "force" to event.force, "material" to (event.bow?.type?.name ?: "AIR"), "projectile_id" to event.projectile.uniqueId.toString())) }
}
