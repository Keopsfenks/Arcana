package com.keopsfenks.arcana.skills.components.triggers
import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.skills.execution.SkillDamageScope
import org.bukkit.entity.*
import org.bukkit.event.*
import org.bukkit.event.entity.*
class EntityAttackSkillTrigger(plugin: ArcanaPlugin) : BukkitPlatformTrigger("entity_attack", plugin, setOf("damage", "damage_source_id")) {
    override fun supportsInterception() = true

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEvent(event: EntityDamageByEntityEvent) {
        if (event.isCancelled || SkillDamageScope.isActive() || event.cause !in setOf(EntityDamageEvent.DamageCause.ENTITY_ATTACK, EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK)) return
        if (event.damager !is LivingEntity || event.damager is Player) return
        val input = com.keopsfenks.arcana.skills.platform.events.DamageEventInputs.capture(event, attack = true, final = true) ?: return
        if (input.actor().entity() is Player) return
        emit(input)
    }
}
