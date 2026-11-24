package com.keopsfenks.arcana.skills.components.providers

import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.execution.SkillScheduler
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import kotlin.math.floor

class ExperienceResourceProvider(scheduler: SkillScheduler) : NativeResourceProvider(scheduler, SkillDescriptor.of("experience", "Current vanilla experience-point cost"), true) {
    override fun validateAmount(amount: Double) { require(amount.isFinite() && amount in 0.0..Int.MAX_VALUE.toDouble() && floor(amount) == amount) { "Experience cost must be a nonnegative integer point amount" } }
    override fun read(actor: LivingEntity) = VanillaExperience.read(actor as Player).toDouble()
    override fun hold(actor: LivingEntity, amount: Double): SkillResourceReservation {
        val player = actor as Player
        val available = VanillaExperience.read(player)
        if (available < amount) return insufficient()
        VanillaExperience.write(player, available - amount.toInt())
        return held(actor) { VanillaExperience.write(player, (VanillaExperience.read(player).toLong() + amount.toLong()).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()) }
    }
}
