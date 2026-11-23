package com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity

import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalMeleeAttack
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.EntityGoalFactory
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal

object MeleeAttackGoalFactory : EntityGoalFactory<EntityGoalMeleeAttack> {
    override fun create(apiGoal: EntityGoalMeleeAttack, entity: PathfinderMob): Goal {
        return MeleeAttackGoal(
            entity,
            apiGoal.speed,
            apiGoal.pauseWhenMobIdle
        )
    }

    override fun isGoalOfType(goal: Goal) = goal is MeleeAttackGoal
}
