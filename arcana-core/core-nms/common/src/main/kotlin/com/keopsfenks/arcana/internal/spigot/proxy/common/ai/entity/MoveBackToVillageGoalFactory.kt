package com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity

import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalMoveBackToVillage
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.EntityGoalFactory
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.ai.goal.MoveBackToVillageGoal

object MoveBackToVillageGoalFactory : EntityGoalFactory<EntityGoalMoveBackToVillage> {
    override fun create(apiGoal: EntityGoalMoveBackToVillage, entity: PathfinderMob): Goal {
        return MoveBackToVillageGoal(
            entity,
            apiGoal.speed,
            apiGoal.canDespawn
        )
    }

    override fun isGoalOfType(goal: Goal) = goal is MoveBackToVillageGoal
}
