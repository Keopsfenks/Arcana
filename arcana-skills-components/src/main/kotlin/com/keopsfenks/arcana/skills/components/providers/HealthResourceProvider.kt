package com.keopsfenks.arcana.skills.components.providers

import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.execution.SkillScheduler
import org.bukkit.entity.LivingEntity

class HealthResourceProvider(scheduler: SkillScheduler) : NativeResourceProvider(scheduler, SkillDescriptor.of("health", "Health-point cost that leaves at least one health point"), false) {
    override fun read(actor: LivingEntity) = actor.health
    override fun hold(actor: LivingEntity, amount: Double): SkillResourceReservation {
        if (actor.health - amount < 1.0) return insufficient()
        actor.health -= amount
        return held(actor) {
            @Suppress("DEPRECATION")
            if (actor.health > 0) actor.health = minOf(actor.maxHealth, actor.health + amount)
        }
    }
}
