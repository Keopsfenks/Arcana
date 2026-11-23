package com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity

import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalUseItem
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.EntityGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.asNMSStack
import com.keopsfenks.arcana.internal.spigot.proxy.common.toBukkitEntity
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.ai.goal.UseItemGoal
import org.bukkit.craftbukkit.CraftSound

object UseItemGoalFactory : EntityGoalFactory<EntityGoalUseItem> {
    override fun create(apiGoal: EntityGoalUseItem, entity: PathfinderMob): Goal {
        // Not SoundEvent(key, ...): its key type became Identifier at 1.21.11, breaking every
        // version this module is relocated into from then on.
        return UseItemGoal(
            entity,
            apiGoal.item.asNMSStack(),
            CraftSound.bukkitToMinecraft(apiGoal.sound),
        ) {
            apiGoal.condition.test(it.toBukkitEntity())
        }
    }

    override fun isGoalOfType(goal: Goal) = goal is UseItemGoal<*>
}
