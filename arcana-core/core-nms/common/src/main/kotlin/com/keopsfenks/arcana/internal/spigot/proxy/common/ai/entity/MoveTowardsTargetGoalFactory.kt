package com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity

import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalMoveTowardsTarget
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.EntityGoalFactory
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal

object MoveTowardsTargetGoalFactory : EntityGoalFactory<EntityGoalMoveTowardsTarget> {
    override fun create(apiGoal: EntityGoalMoveTowardsTarget, entity: PathfinderMob): Goal {
        return MoveTowardsTargetGoal(
            entity,
            apiGoal.speed,
            apiGoal.maxDistance.toFloat()
        )
    }

    override fun isGoalOfType(goal: Goal) = goal is MoveTowardsTargetGoal
}
