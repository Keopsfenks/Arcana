package com.keopsfenks.arcana.skills.components.triggers
import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.skills.execution.SkillDamageScope
import org.bukkit.entity.LivingEntity
import org.bukkit.event.*
import org.bukkit.event.entity.EntityDeathEvent
class KillSkillTrigger(plugin: ArcanaPlugin) : BukkitPlatformTrigger("kill", plugin, setOf("killed_type", "damage_source_id")) {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onEvent(event: EntityDeathEvent) {
        if (SkillDamageScope.isActive()) return
        val actor = event.damageSource.causingEntity as? LivingEntity ?: return
        val data = linkedMapOf<String, Any>("killed_type" to event.entity.type.name)
        event.damageSource.directEntity?.let { data["damage_source_id"] = it.uniqueId.toString() }
        emit(actor, event.entity, data)
    }
}
