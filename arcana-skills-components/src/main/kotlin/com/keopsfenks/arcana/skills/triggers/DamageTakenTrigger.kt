package com.keopsfenks.arcana.skills.triggers

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.skills.SkillContext
import com.keopsfenks.arcana.skills.execution.SkillDamageScope
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent

class DamageTakenTrigger(plugin: ArcanaPlugin) : BukkitSkillTrigger("damage_taken", plugin) {
    override fun supportsInterception() = true

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEvent(event: EntityDamageEvent) {
        if (SkillDamageScope.isActive() || event.isCancelled) return
        val player = event.entity as? Player ?: return
        val source = (event as? EntityDamageByEntityEvent)?.damager
        val counterpart = if (source is Projectile) source.shooter as? LivingEntity else source as? LivingEntity
        val damage = event.finalDamage
        if (damage.isFinite()) {
            val builder = SkillContext.builder(player).victim(counterpart).value("damage", damage).value("cause", event.cause.name)
            source?.let { builder.value("damage_source_id", it.uniqueId.toString()) }
            emit(builder.build())
        }
    }
}
