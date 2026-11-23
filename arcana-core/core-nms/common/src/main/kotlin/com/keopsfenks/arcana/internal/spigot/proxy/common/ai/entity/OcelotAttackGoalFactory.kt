package com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity

import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalOcelotAttack
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.EntityGoalFactory
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.ai.goal.OcelotAttackGoal

object OcelotAttackGoalFactory : EntityGoalFactory<EntityGoalOcelotAttack> {
    override fun create(apiGoal: EntityGoalOcelotAttack, entity: PathfinderMob): Goal {
        return OcelotAttackGoal(
            entity
        )
    }

    override fun isGoalOfType(goal: Goal) = goal is OcelotAttackGoal
}
