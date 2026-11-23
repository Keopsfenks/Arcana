package com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity

import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalRandomSwimming
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.EntityGoalFactory
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal

object RandomSwimmingGoalFactory : EntityGoalFactory<EntityGoalRandomSwimming> {
    override fun create(apiGoal: EntityGoalRandomSwimming, entity: PathfinderMob): Goal {
        return RandomSwimmingGoal(
            entity,
            apiGoal.speed,
            apiGoal.interval
        )
    }

    override fun isGoalOfType(goal: Goal) = goal is RandomSwimmingGoal
}
