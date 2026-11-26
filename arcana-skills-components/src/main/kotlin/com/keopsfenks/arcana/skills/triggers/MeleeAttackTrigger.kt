package com.keopsfenks.arcana.skills.triggers

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.skills.SkillContext
import com.keopsfenks.arcana.skills.execution.SkillDamageScope
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent.DamageCause

class MeleeAttackTrigger(plugin: ArcanaPlugin) : BukkitSkillTrigger("melee_attack", plugin) {
    override fun supportsInterception() = true

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEvent(event: EntityDamageByEntityEvent) {
        if (SkillDamageScope.isActive() || event.isCancelled) return
        val cause = event.cause
        if (cause != DamageCause.ENTITY_ATTACK && cause != DamageCause.ENTITY_SWEEP_ATTACK) return
        val player = event.damager as? Player ?: return
        val victim = event.entity as? LivingEntity ?: return
        val damage = event.finalDamage
        if (damage.isFinite()) emit(SkillContext.builder(player).victim(victim).value("damage", damage).value("cause", event.cause.name).value("entity", victim.type.name).build())
    }
}
