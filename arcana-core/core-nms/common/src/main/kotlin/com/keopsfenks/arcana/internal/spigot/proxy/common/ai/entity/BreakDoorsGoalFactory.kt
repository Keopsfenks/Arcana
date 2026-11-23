package com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity

import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalBreakDoors
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.EntityGoalFactory
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.BreakDoorGoal
import net.minecraft.world.entity.ai.goal.Goal

object BreakDoorsGoalFactory : EntityGoalFactory<EntityGoalBreakDoors> {
    override fun create(apiGoal: EntityGoalBreakDoors, entity: PathfinderMob): Goal {
        return BreakDoorGoal(
            entity,
            apiGoal.ticks
        ) { true }
    }

    override fun isGoalOfType(goal: Goal) = goal is BreakDoorGoal
}
