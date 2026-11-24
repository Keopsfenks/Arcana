package com.keopsfenks.arcana.skills.components.providers

import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.execution.SkillScheduler
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import kotlin.math.floor

class FoodResourceProvider(scheduler: SkillScheduler) : NativeResourceProvider(scheduler, SkillDescriptor.of("food", "Whole food-point cost with saturation-aware compensation"), true) {
    override fun validateAmount(amount: Double) { require(amount.isFinite() && amount in 0.0..20.0 && floor(amount) == amount) { "Food amount must be an integer between zero and 20" } }
    override fun read(actor: LivingEntity) = (actor as Player).foodLevel.toDouble()
    override fun hold(actor: LivingEntity, amount: Double): SkillResourceReservation {
        val player = actor as Player
        val points = amount.toInt()
        if (player.foodLevel < points) return insufficient()
        val saturation = player.saturation.also { require(it.isFinite() && it >= 0) { "Food saturation must be finite and nonnegative" } }
        player.foodLevel -= points
        val remaining = minOf(saturation, player.foodLevel.toFloat())
        player.saturation = remaining
        val removedSaturation = saturation - remaining
        return held(actor) {
            player.foodLevel = (player.foodLevel + points).coerceAtMost(20)
            player.saturation = minOf(player.foodLevel.toFloat(), player.saturation + removedSaturation)
        }
    }
}
