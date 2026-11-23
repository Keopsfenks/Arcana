package com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity

import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalRangedAttack
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.EntityGoalFactory
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.ai.goal.RangedAttackGoal
import net.minecraft.world.entity.monster.RangedAttackMob

object RangedAttackGoalFactory : EntityGoalFactory<EntityGoalRangedAttack> {
    override fun create(apiGoal: EntityGoalRangedAttack, entity: PathfinderMob): Goal? {
        return RangedAttackGoal(
            entity as? RangedAttackMob ?: return null,
            apiGoal.speed,
            apiGoal.minInterval,
            apiGoal.maxInterval,
            apiGoal.maxRange.toFloat()
        )
    }

    override fun isGoalOfType(goal: Goal) = goal is RangedAttackGoal
}
