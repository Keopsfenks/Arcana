package com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity

import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalStrollThroughVillage
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.EntityGoalFactory
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.ai.goal.StrollThroughVillageGoal

object StrollThroughVillageGoalFactory : EntityGoalFactory<EntityGoalStrollThroughVillage> {
    override fun create(apiGoal: EntityGoalStrollThroughVillage, entity: PathfinderMob): Goal {
        return StrollThroughVillageGoal(
            entity,
            apiGoal.searchRange
        )
    }

    override fun isGoalOfType(goal: Goal) = goal is StrollThroughVillageGoal
}
