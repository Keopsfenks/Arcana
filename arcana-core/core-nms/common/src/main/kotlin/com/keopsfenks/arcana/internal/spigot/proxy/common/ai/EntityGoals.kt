package com.keopsfenks.arcana.internal.spigot.proxy.common.ai

import com.keopsfenks.arcana.core.entities.ai.CustomGoal
import com.keopsfenks.arcana.core.entities.ai.EntityGoal
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalAvoidEntity
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalBreakDoors
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalBreatheAir
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalBreed
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalCatLieOnBed
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalCatSitOnBed
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalEatGrass
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalFleeSun
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalFloat
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalFollowBoats
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalFollowMobs
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalIllusionerBlindnessSpell
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalIllusionerMirrorSpell
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalInteract
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalLeapAtTarget
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalLookAtPlayer
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalMeleeAttack
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalMoveBackToVillage
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalMoveThroughVillage
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalMoveTowardsRestriction
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalMoveTowardsTarget
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalOcelotAttack
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalOpenDoors
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalPanic
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalRandomLookAround
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalRandomStroll
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalRandomSwimming
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalRangedAttack
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalRangedBowAttack
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalRangedCrossbowAttack
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalRestrictSun
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalStrollThroughVillage
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalTempt
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalTryFindWater
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalUseItem
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalWaterAvoidingRandomFlying
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalWaterAvoidingRandomStroll
import com.keopsfenks.arcana.core.entities.ai.entity.EntityGoalWolfBeg
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.AvoidEntityGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.BreakDoorsGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.BreatheAirGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.BreedGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.CatLieOnBedGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.CatSitOnBedGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.EatGrassGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.FleeSunGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.FloatGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.FollowBoatsGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.FollowMobsGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.IllusionerBlindnessSpellGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.IllusionerMirrorSpellGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.InteractGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.LeapAtTargetGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.LookAtPlayerGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.MeleeAttackGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.MoveBackToVillageGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.MoveThroughVillageGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.MoveTowardsRestrictionGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.MoveTowardsTargetGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.OcelotAttackGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.OpenDoorsGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.PanicGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.RandomLookAroundGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.RandomStrollGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.RandomSwimmingGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.RangedAttackGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.RangedBowAttackGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.RangedCrossbowAttackGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.RestrictSunGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.StrollThroughVillageGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.TemptGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.TryFindWaterGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.UseItemGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.WaterAvoidingRandomFlyingGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.WaterAvoidingRandomStrollGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.ai.entity.WolfBegGoalFactory
import com.keopsfenks.arcana.internal.spigot.proxy.common.getVersionSpecificEntityGoalFactory
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.Goal

fun <T : EntityGoal<*>> T.getGoalFactory(): EntityGoalFactory<T>? {
    val versionSpecific = this.getVersionSpecificEntityGoalFactory()
    if (versionSpecific != null) {
        return versionSpecific
    }

    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is EntityGoalAvoidEntity -> AvoidEntityGoalFactory
        is EntityGoalBreakDoors -> BreakDoorsGoalFactory
        is EntityGoalBreatheAir -> BreatheAirGoalFactory
        is EntityGoalEatGrass -> EatGrassGoalFactory
        is EntityGoalFleeSun -> FleeSunGoalFactory
        is EntityGoalFloat -> FloatGoalFactory
        is EntityGoalFollowBoats -> FollowBoatsGoalFactory
        is EntityGoalFollowMobs -> FollowMobsGoalFactory
        is EntityGoalInteract -> InteractGoalFactory
        is EntityGoalLeapAtTarget -> LeapAtTargetGoalFactory
        is EntityGoalLookAtPlayer -> LookAtPlayerGoalFactory
        is EntityGoalMeleeAttack -> MeleeAttackGoalFactory
        is EntityGoalMoveBackToVillage -> MoveBackToVillageGoalFactory
        is EntityGoalMoveThroughVillage -> MoveThroughVillageGoalFactory
        is EntityGoalMoveTowardsRestriction -> MoveTowardsRestrictionGoalFactory
        is EntityGoalMoveTowardsTarget -> MoveTowardsTargetGoalFactory
        is EntityGoalOcelotAttack -> OcelotAttackGoalFactory
        is EntityGoalOpenDoors -> OpenDoorsGoalFactory
        is EntityGoalPanic -> PanicGoalFactory
        is EntityGoalRandomLookAround -> RandomLookAroundGoalFactory
        is EntityGoalRandomStroll -> RandomStrollGoalFactory
        is EntityGoalRandomSwimming -> RandomSwimmingGoalFactory
        is EntityGoalRangedAttack -> RangedAttackGoalFactory
        is EntityGoalRangedBowAttack -> RangedBowAttackGoalFactory
        is EntityGoalRangedCrossbowAttack -> RangedCrossbowAttackGoalFactory
        is EntityGoalRestrictSun -> RestrictSunGoalFactory
        is EntityGoalStrollThroughVillage -> StrollThroughVillageGoalFactory
        is EntityGoalTempt -> TemptGoalFactory
        is EntityGoalTryFindWater -> TryFindWaterGoalFactory
        is EntityGoalUseItem -> UseItemGoalFactory
        is EntityGoalWaterAvoidingRandomFlying -> WaterAvoidingRandomFlyingGoalFactory
        is EntityGoalWaterAvoidingRandomStroll -> WaterAvoidingRandomStrollGoalFactory
        is EntityGoalWolfBeg -> WolfBegGoalFactory
        is EntityGoalBreed -> BreedGoalFactory
        is EntityGoalCatSitOnBed -> CatSitOnBedGoalFactory
        is EntityGoalCatLieOnBed -> CatLieOnBedGoalFactory
        is EntityGoalIllusionerBlindnessSpell -> IllusionerBlindnessSpellGoalFactory
        is EntityGoalIllusionerMirrorSpell -> IllusionerMirrorSpellGoalFactory
        is CustomGoal<*> -> CustomGoalFactory
        else -> null
    } as EntityGoalFactory<T>?
}

interface EntityGoalFactory<T : EntityGoal<*>> {
    fun create(apiGoal: T, entity: PathfinderMob): Goal?
    fun isGoalOfType(goal: Goal): Boolean
}
