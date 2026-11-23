package com.keopsfenks.arcana.internal.spigot.proxy.common.ai.target

import com.keopsfenks.arcana.core.entities.ai.target.TargetGoalOwnerHurtBy
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.TargetGoalFactory
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.TamableAnimal
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal

object OwnerHurtByGoalFactory : TargetGoalFactory<TargetGoalOwnerHurtBy> {
    override fun create(apiGoal: TargetGoalOwnerHurtBy, entity: PathfinderMob): Goal? {
        return OwnerHurtByTargetGoal(
            entity as? TamableAnimal ?: return null
        )
    }

    override fun isGoalOfType(goal: Goal) = goal is OwnerHurtByTargetGoal
}
