package com.keopsfenks.arcana.skills

import com.keopsfenks.arcana.core.skills.SkillCondition
import com.keopsfenks.arcana.core.skills.SkillEffect
import com.keopsfenks.arcana.core.skills.SkillEngine
import com.keopsfenks.arcana.skills.conditions.*
import com.keopsfenks.arcana.skills.effects.*
import org.bukkit.plugin.Plugin
import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.skills.SkillTrigger
import com.keopsfenks.arcana.skills.triggers.*

/** The composition root only lists components; dispatch is registry driven. */
object BuiltinComponents {
    fun effectTypes(): List<SkillEffect> = listOf(
        MessageEffect(), ActionBarEffect(), SoundEffect(), ParticleEffect(), HealEffect(), DamageEffect(),
        FeedEffect(), IgniteEffect(), PotionEffect(), RemovePotionEffect(), VelocityEffect(), ConsoleCommandEffect(),
        PlayerCommandEffect(), SequenceEffect(), DelayEffect(), RepeatEffect(), BranchEffect(), CastEffect()
    )

    fun conditionTypes(): List<SkillCondition> = listOf(
        PermissionCondition(), WorldCondition(), HealthBelowCondition(), HealthAboveCondition(), FoodBelowCondition(),
        FoodAboveCondition(), SneakingCondition(), SprintingCondition(), HasTargetCondition(), ExpressionCondition(),
        AllCondition(), AnyCondition(), NotCondition()
    )

    fun install(engine: SkillEngine, owner: Plugin) {
        effectTypes().forEach { engine.registerEffect(owner, it) }
        conditionTypes().forEach { engine.registerCondition(owner, it) }
    }

    fun triggerTypes(plugin: ArcanaPlugin): List<SkillTrigger> = listOf(
        JoinTrigger(plugin), RespawnTrigger(plugin), DeathTrigger(plugin), BlockBreakTrigger(plugin), BlockPlaceTrigger(plugin),
        MeleeAttackTrigger(plugin), ProjectileHitTrigger(plugin), DamageTakenTrigger(plugin), RightClickTrigger(plugin),
        LeftClickTrigger(plugin), SneakTrigger(plugin), SprintTrigger(plugin)
    )

    fun installTriggers(engine: SkillEngine, owner: ArcanaPlugin) {
        triggerTypes(owner).forEach { engine.registerTrigger(owner, it) }
    }
}
