package com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity

import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalWolfBeg
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.EntityGoalFactory
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.BegGoal
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.animal.wolf.Wolf

object WolfBegGoalFactory : EntityGoalFactory<EntityGoalWolfBeg> {
    override fun create(apiGoal: EntityGoalWolfBeg, entity: PathfinderMob): Goal? {
        return BegGoal(
            entity as? Wolf ?: return null,
            apiGoal.distance.toFloat()
        )
    }

    override fun isGoalOfType(goal: Goal) = goal is BegGoal
}
