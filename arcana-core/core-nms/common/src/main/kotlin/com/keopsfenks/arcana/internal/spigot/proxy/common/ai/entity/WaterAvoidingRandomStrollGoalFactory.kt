package com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity

import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalWaterAvoidingRandomStroll
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.EntityGoalFactory
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal

object WaterAvoidingRandomStrollGoalFactory : EntityGoalFactory<EntityGoalWaterAvoidingRandomStroll> {
    override fun create(apiGoal: EntityGoalWaterAvoidingRandomStroll, entity: PathfinderMob): Goal {
        return WaterAvoidingRandomStrollGoal(
            entity,
            apiGoal.speed,
            apiGoal.chance.toFloat()
        )
    }

    override fun isGoalOfType(goal: Goal) = goal is WaterAvoidingRandomStrollGoal
}

