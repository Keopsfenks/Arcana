package com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity

import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalFollowBoats
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.EntityGoalFactory
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.FollowBoatGoal
import net.minecraft.world.entity.ai.goal.Goal

object FollowBoatsGoalFactory : EntityGoalFactory<EntityGoalFollowBoats> {
    override fun create(apiGoal: EntityGoalFollowBoats, entity: PathfinderMob): Goal {
        return FollowBoatGoal(
            entity
        )
    }

    override fun isGoalOfType(goal: Goal) = goal is FollowBoatGoal
}
