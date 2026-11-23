package com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity

import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalOpenDoors
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.EntityGoalFactory
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.ai.goal.OpenDoorGoal

object OpenDoorsGoalFactory : EntityGoalFactory<EntityGoalOpenDoors> {
    override fun create(apiGoal: EntityGoalOpenDoors, entity: PathfinderMob): Goal {
        return OpenDoorGoal(
            entity,
            apiGoal.delayClosing
        )
    }

    override fun isGoalOfType(goal: Goal) = goal is OpenDoorGoal
}
