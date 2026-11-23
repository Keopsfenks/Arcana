package com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity

import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalBreed
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.EntityGoalFactory
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.BreedGoal
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.animal.Animal

object BreedGoalFactory : EntityGoalFactory<EntityGoalBreed> {
    override fun create(apiGoal: EntityGoalBreed, entity: PathfinderMob): Goal? {
        return BreedGoal(
            entity as? Animal ?: return null,
            apiGoal.speed
        )
    }

    override fun isGoalOfType(goal: Goal) = goal is BreedGoal
}
