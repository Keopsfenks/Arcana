package com.keopsfenks.arcana.internal.spigot.proxy.v26_1_2.common.ai.entity

import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalIllusionerMirrorSpell
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.EntityGoalFactory
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.monster.illager.Illusioner

object IllusionerMirrorSpellGoalFactory : EntityGoalFactory<EntityGoalIllusionerMirrorSpell> {
    override fun create(apiGoal: EntityGoalIllusionerMirrorSpell, entity: PathfinderMob): Goal? {
        if (entity !is Illusioner) return null

        // Have to use reflection for it to work
        return Illusioner::class.java.declaredClasses[0]
            .getDeclaredConstructor(Illusioner::class.java)
            .apply { isAccessible = true }
            .newInstance(entity) as Goal
    }

    override fun isGoalOfType(goal: Goal): Boolean {
        return Illusioner::class.java.declaredClasses[0].isInstance(goal)
    }
}
