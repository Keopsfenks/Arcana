package com.keopsfenks.arcana.internal.spigot.proxy.common.ai

import com.keopsfenks.arcana.core.entities.ai.CustomGoal
import com.keopsfenks.arcana.core.entities.ai.TargetGoal
import com.keopsfenks.arcana.core.entities.ai.target.TargetGoalHurtBy
import com.keopsfenks.arcana.core.entities.ai.target.TargetGoalNearestAttackable
import com.keopsfenks.arcana.core.entities.ai.target.TargetGoalNearestAttackableWitch
import com.keopsfenks.arcana.core.entities.ai.target.TargetGoalNearestHealableRaider
import com.keopsfenks.arcana.core.entities.ai.target.TargetGoalNonTameRandom
import com.keopsfenks.arcana.core.entities.ai.target.TargetGoalOwnerHurtBy
import com.keopsfenks.arcana.core.entities.ai.target.TargetGoalOwnerTarget
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.target.HurtByGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.target.NearestAttackableGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.target.NearestAttackableWitchGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.target.NearestHealableRaiderGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.target.NonTameRandomGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.target.OwnerHurtByGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.target.OwnerTargetGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.getVersionSpecificEntityGoalFactory
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.Goal

fun <T : TargetGoal<*>> T.getGoalFactory(): TargetGoalFactory<T>? {
    val versionSpecific = this.getVersionSpecificEntityGoalFactory()
    if (versionSpecific != null) {
        return versionSpecific
    }

    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is TargetGoalHurtBy -> HurtByGoalFactory
        is TargetGoalNearestAttackable -> NearestAttackableGoalFactory
        is TargetGoalNearestAttackableWitch -> NearestAttackableWitchGoalFactory
        is TargetGoalNearestHealableRaider -> NearestHealableRaiderGoalFactory
        is TargetGoalNonTameRandom -> NonTameRandomGoalFactory
        is TargetGoalOwnerHurtBy -> OwnerHurtByGoalFactory
        is TargetGoalOwnerTarget -> OwnerTargetGoalFactory
        is CustomGoal<*> -> CustomGoalFactory
        else -> null
    } as TargetGoalFactory<T>?
}

interface TargetGoalFactory<T : TargetGoal<*>> {
    fun create(apiGoal: T, entity: PathfinderMob): Goal?
    fun isGoalOfType(goal: Goal): Boolean
}
