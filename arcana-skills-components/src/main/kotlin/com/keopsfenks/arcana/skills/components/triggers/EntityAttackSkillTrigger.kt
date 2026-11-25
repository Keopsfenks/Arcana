package com.keopsfenks.arcana.skills.components.triggers
import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.skills.execution.SkillDamageScope
import org.bukkit.entity.*
import org.bukkit.event.*
import org.bukkit.event.entity.*
class EntityAttackSkillTrigger(plugin: ArcanaPlugin) : BukkitPlatformTrigger("entity_attack", plugin, setOf("damage", "damage_source_id")) {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEvent(event: EntityDamageByEntityEvent) {
        if (event.isCancelled || SkillDamageScope.isActive() || event.cause !in setOf(EntityDamageEvent.DamageCause.ENTITY_ATTACK, EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK)) return
        val attacker = event.damager as? LivingEntity ?: return; if (attacker is Player) return
        val target = event.entity as? LivingEntity ?: return; val damage = event.finalDamage
        if (damage.isFinite()) emit(attacker, target, mapOf("damage" to damage))
    }
}
