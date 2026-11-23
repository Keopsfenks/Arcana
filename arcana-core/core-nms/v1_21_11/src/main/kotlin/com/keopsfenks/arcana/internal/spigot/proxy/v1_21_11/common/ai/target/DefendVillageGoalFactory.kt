package com.keopsfenks.arcana.internal.spigot.proxy.v1_21_11.common.ai.target

import com.keopsfenks.arcana.core.entities.ai.target.TargetGoalDefendVillage
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.TargetGoalFactory
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.ai.goal.target.DefendVillageTargetGoal
import net.minecraft.world.entity.animal.golem.IronGolem

object DefendVillageGoalFactory : TargetGoalFactory<TargetGoalDefendVillage> {
    override fun create(apiGoal: TargetGoalDefendVillage, entity: PathfinderMob): Goal? {
        return DefendVillageTargetGoal(
            entity as? IronGolem ?: return null
        )
    }

    override fun isGoalOfType(goal: Goal) = goal is DefendVillageTargetGoal
}
