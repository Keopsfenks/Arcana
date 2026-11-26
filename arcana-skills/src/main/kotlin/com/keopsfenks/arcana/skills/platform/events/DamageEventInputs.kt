package com.keopsfenks.arcana.skills.platform.events

import com.keopsfenks.arcana.core.skills.platform.*
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Projectile
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent

/** Copies event values and entity identities only; live counterpart snapshots require its owner. */
object DamageEventInputs {
    fun capture(event: EntityDamageEvent, attack: Boolean = false, final: Boolean = false): SkillInvocation? {
        val victim = event.entity as? LivingEntity ?: return null
        val source = (event as? EntityDamageByEntityEvent)?.damager
        val counterpart = if (source is Projectile) source.shooter as? LivingEntity else source as? LivingEntity
        if (attack && (counterpart == null || source is Projectile || event.cause !in setOf(EntityDamageEvent.DamageCause.ENTITY_ATTACK, EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK))) return null
        val damage = if (final) event.finalDamage else event.damage
        if (!damage.isFinite() || damage < 0) return null
        val actor = if (attack) counterpart!! else victim
        val other = (if (attack) victim else counterpart)?.let(SkillActor::of)
        val data = linkedMapOf<String, Any>("damage" to damage, "cause" to event.cause.name)
        source?.let { data["damage_source_id"] = it.uniqueId.toString() }
        return SkillInvocation.builder(SkillActor.of(actor)).target(other).trigger(other).data(data).build()
    }
}
