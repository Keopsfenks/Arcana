package com.keopsfenks.arcana.skills.components.triggers
import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.skills.execution.SkillDamageScope
import org.bukkit.entity.*
import org.bukkit.event.*
import org.bukkit.event.entity.*
class EntityDamagedSkillTrigger(plugin: ArcanaPlugin) : BukkitPlatformTrigger("entity_damaged", plugin, setOf("damage", "cause", "damage_source_id")) {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEvent(event: EntityDamageEvent) {
        if (event.isCancelled || SkillDamageScope.isActive()) return
        val victim = event.entity as? LivingEntity ?: return; if (victim is Player) return
        val source = (event as? EntityDamageByEntityEvent)?.damager
        val other = if (source is Projectile) source.shooter as? LivingEntity else source as? LivingEntity
        val damage = event.finalDamage; if (!damage.isFinite()) return
        val data = linkedMapOf<String, Any>("damage" to damage, "cause" to event.cause.name)
        source?.let { data["damage_source_id"] = it.uniqueId.toString() }
        emit(victim, other, data)
    }
}
