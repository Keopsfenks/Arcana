package com.keopsfenks.arcana.internal.spigot.proxy.common.ai.target

import com.keopsfenks.arcana.core.entities.ai.target.TargetGoalOwnerTarget
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.TargetGoalFactory
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.TamableAnimal
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal

object OwnerTargetGoalFactory : TargetGoalFactory<TargetGoalOwnerTarget> {
    override fun create(apiGoal: TargetGoalOwnerTarget, entity: PathfinderMob): Goal? {
        return OwnerHurtTargetGoal(
            entity as? TamableAnimal ?: return null
        )
    }

    override fun isGoalOfType(goal: Goal) = goal is OwnerHurtTargetGoal
}
