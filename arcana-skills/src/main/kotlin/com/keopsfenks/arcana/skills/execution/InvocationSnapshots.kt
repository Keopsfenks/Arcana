package com.keopsfenks.arcana.skills.execution

import com.keopsfenks.arcana.core.skills.SkillContext
import org.bukkit.entity.Player
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutionException

object InvocationSnapshots {
    @Suppress("DEPRECATION")
    fun capture(input: SkillContext, state: ExecutionState, scheduler: SkillScheduler): CompletableFuture<SkillContext> =
        state.track(scheduler.on(input.caster) {
            state.checkActive()
            val caster = input.caster
            if (!caster.isOnline) throw CancellationException("Caster is offline")
            input.toBuilder()
                .value("damage", input.getNumber("damage"))
                .value("caster_name", caster.name)
                .value("caster_health", caster.health)
                .value("caster_max_health", caster.maxHealth)
                .value("caster_food", caster.foodLevel)
                .value("caster_level", caster.level)
                .value("caster_sneaking", caster.isSneaking)
                .value("caster_sprinting", caster.isSprinting)
                .value("world", caster.world.name)
                .value("item", caster.inventory.itemInMainHand.type.name)
                .build()
        }).thenCompose { context ->
            val victim = context.victim
            if (victim == null) CompletableFuture.completedFuture(context)
            else state.track(scheduler.on(victim) {
                state.checkActive()
                val valid = if (victim is Player) victim.isOnline else victim.isValid
                if (!valid) context.toBuilder().victim(null).build()
                else context.toBuilder().value("target_name", victim.name).value("entity", victim.type.name).build()
            }).handle { captured, error ->
                state.checkActive()
                if (error == null) captured
                else {
                    var cause = error
                    while ((cause is CompletionException || cause is ExecutionException) && cause.cause != null) cause = cause.cause!!
                    if (cause is CancellationException) context.toBuilder().victim(null).build()
                    else throw CompletionException(cause)
                }
            }
        }
}
