package com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity

import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalMoveTowardsRestriction
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.EntityGoalFactory
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal

object MoveTowardsRestrictionGoalFactory : EntityGoalFactory<EntityGoalMoveTowardsRestriction> {
    override fun create(apiGoal: EntityGoalMoveTowardsRestriction, entity: PathfinderMob): Goal {
        return MoveTowardsRestrictionGoal(
            entity,
            apiGoal.speed
        )
    }

    override fun isGoalOfType(goal: Goal) = goal is MoveTowardsRestrictionGoal
}
