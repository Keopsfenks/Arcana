package com.keopsfenks.arcana.internal.spigot.proxy.v1_21_11.common.ai.entity

import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalCatSitOnBed
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.EntityGoalFactory
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.CatSitOnBlockGoal
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.animal.feline.Cat

object CatSitOnBedGoalFactory : EntityGoalFactory<EntityGoalCatSitOnBed> {
    override fun create(apiGoal: EntityGoalCatSitOnBed, entity: PathfinderMob): Goal? {
        return CatSitOnBlockGoal(
            entity as? Cat ?: return null,
            apiGoal.speed
        )
    }

    override fun isGoalOfType(goal: Goal) = goal is CatSitOnBlockGoal
}
