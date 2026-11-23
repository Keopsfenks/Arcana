package com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity

import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalCatLieOnBed
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.EntityGoalFactory
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.CatLieOnBedGoal
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.animal.Cat

object CatLieOnBedGoalFactory : EntityGoalFactory<EntityGoalCatLieOnBed> {
    override fun create(apiGoal: EntityGoalCatLieOnBed, entity: PathfinderMob): Goal? {
        return CatLieOnBedGoal(
            entity as? Cat ?: return null,
            apiGoal.speed,
            apiGoal.range
        )
    }

    override fun isGoalOfType(goal: Goal) = goal is CatLieOnBedGoal
}
