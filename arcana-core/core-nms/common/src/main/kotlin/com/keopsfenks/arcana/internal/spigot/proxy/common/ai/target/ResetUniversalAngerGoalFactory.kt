package com.keopsfenks.arcana.internal.spigot.proxy.common.ai.target

import com.keopsfenks.arcana.core.entities.ai.target.TargetGoalResetUniversalAnger
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.TargetGoalFactory
import net.minecraft.world.entity.NeutralMob
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal

object ResetUniversalAngerGoalFactory : TargetGoalFactory<TargetGoalResetUniversalAnger> {
    override fun create(apiGoal: TargetGoalResetUniversalAnger, entity: PathfinderMob): Goal? {
        if (entity !is NeutralMob) return null

        return ResetUniversalAngerTargetGoal(
            entity,
            apiGoal.triggerOthers
        )
    }

    override fun isGoalOfType(goal: Goal) = goal is ResetUniversalAngerTargetGoal<*>
}
