package com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity

import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalMoveThroughVillage
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.EntityGoalFactory
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.ai.goal.MoveThroughVillageGoal

object MoveThroughVillageGoalFactory : EntityGoalFactory<EntityGoalMoveThroughVillage> {
    override fun create(apiGoal: EntityGoalMoveThroughVillage, entity: PathfinderMob): Goal {
        return MoveThroughVillageGoal(
            entity,
            apiGoal.speed,
            apiGoal.onlyAtNight,
            apiGoal.distance
        ) { apiGoal.canPassThroughDoors }
    }

    override fun isGoalOfType(goal: Goal) = goal is MoveThroughVillageGoal
}
