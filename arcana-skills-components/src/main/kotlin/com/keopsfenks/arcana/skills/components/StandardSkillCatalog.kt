package com.keopsfenks.arcana.skills.components
import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.DefaultSkillEngine
import com.keopsfenks.arcana.skills.platform.diagnostics.SkillCatalogEntry
import com.keopsfenks.arcana.skills.components.passives.PotionContributions
import com.keopsfenks.arcana.skills.components.providers.*
import com.keopsfenks.arcana.skills.components.triggers.*

/** Composition only: separate factories, registry dispatch, no file I/O or duplicated implementations. */
class StandardSkillCatalog(private val engine: DefaultSkillEngine, private val owner: ArcanaPlugin) {
    private val platform = engine.platform()
    private val potions = PotionContributions(engine.scheduler)
    private val entries = ArrayList<SkillCatalogEntry>()
    private var installed = false
    fun descriptors(): List<SkillCatalogEntry> = java.util.List.copyOf(entries)
    private fun <T : SkillPlatformComponent> add(type: SkillComponentType<T>, components: List<T>) {
        components.forEach { component ->
            val descriptor = component.descriptor(); val key = SkillKey("arcana", descriptor.id())
            platform.register(owner, type, key, component); entries.add(SkillCatalogEntry(type.id(), key, descriptor))
        }
    }
    fun install() {
        check(!installed) { "Standard catalog already installed" }; installed = true
        platform.own(potions)
        add(SkillComponentType.MECHANIC, listOf(
            com.keopsfenks.arcana.skills.components.effects.auras.AddAuraChargeMechanic(),
            com.keopsfenks.arcana.skills.components.effects.auras.ApplyAuraMechanic(),
            com.keopsfenks.arcana.skills.components.effects.auras.RemoveAuraMechanic(),
            com.keopsfenks.arcana.skills.components.effects.combat.AbsorptionMechanic(),
            com.keopsfenks.arcana.skills.components.effects.combat.DamageMechanic(),
            com.keopsfenks.arcana.skills.components.effects.combat.ExtinguishMechanic(),
            com.keopsfenks.arcana.skills.components.effects.combat.FeedMechanic(),
            com.keopsfenks.arcana.skills.components.effects.combat.HealMechanic(),
            com.keopsfenks.arcana.skills.components.effects.combat.IgniteMechanic(),
            com.keopsfenks.arcana.skills.components.effects.flow.BranchMechanic(),
            com.keopsfenks.arcana.skills.components.effects.flow.CastMechanic(),
            com.keopsfenks.arcana.skills.components.effects.flow.CycleMechanic(),
            com.keopsfenks.arcana.skills.components.effects.flow.DelayMechanic(),
            com.keopsfenks.arcana.skills.components.effects.flow.ForEachMechanic(),
            com.keopsfenks.arcana.skills.components.effects.flow.ParallelMechanic(),
            com.keopsfenks.arcana.skills.components.effects.flow.RepeatMechanic(),
            com.keopsfenks.arcana.skills.components.effects.flow.SequenceMechanic(),
            com.keopsfenks.arcana.skills.components.effects.flow.ShuffleMechanic(),
            com.keopsfenks.arcana.skills.components.effects.flow.WaitUntilMechanic(),
            com.keopsfenks.arcana.skills.components.effects.flow.WeightedRandomMechanic(),
            com.keopsfenks.arcana.skills.components.effects.items.DurabilityMechanic(),
            com.keopsfenks.arcana.skills.components.effects.items.EnchantMechanic(),
            com.keopsfenks.arcana.skills.components.effects.items.GiveItemMechanic(),
            com.keopsfenks.arcana.skills.components.effects.items.RemoveItemMechanic(),
            com.keopsfenks.arcana.skills.components.effects.movement.DashMechanic(),
            com.keopsfenks.arcana.skills.components.effects.movement.KnockbackMechanic(),
            com.keopsfenks.arcana.skills.components.effects.movement.LaunchMechanic(),
            com.keopsfenks.arcana.skills.components.effects.movement.PullMechanic(),
            com.keopsfenks.arcana.skills.components.effects.movement.TeleportMechanic(),
            com.keopsfenks.arcana.skills.components.effects.movement.VelocityMechanic(),
            com.keopsfenks.arcana.skills.components.effects.presentation.ActionBarMechanic(),
            com.keopsfenks.arcana.skills.components.effects.presentation.BossBarMechanic(),
            com.keopsfenks.arcana.skills.components.effects.presentation.ConsoleCommandMechanic(),
            com.keopsfenks.arcana.skills.components.effects.presentation.MessageMechanic(),
            com.keopsfenks.arcana.skills.components.effects.presentation.PlayerCommandMechanic(),
            com.keopsfenks.arcana.skills.components.effects.presentation.TitleMechanic(),
            com.keopsfenks.arcana.skills.components.effects.projectiles.BeamMechanic(),
            com.keopsfenks.arcana.skills.components.effects.projectiles.ChainMechanic(),
            com.keopsfenks.arcana.skills.components.effects.projectiles.MissileMechanic(),
            com.keopsfenks.arcana.skills.components.effects.projectiles.ProjectileMechanic(),
            com.keopsfenks.arcana.skills.components.effects.projectiles.RayMechanic(),
            com.keopsfenks.arcana.skills.components.effects.status.FreezeMechanic(),
            com.keopsfenks.arcana.skills.components.effects.status.PotionMechanic(potions),
            com.keopsfenks.arcana.skills.components.effects.status.RemovePotionMechanic(potions),
            com.keopsfenks.arcana.skills.components.effects.status.StunMechanic(),
            com.keopsfenks.arcana.skills.components.effects.variables.AddVariableMechanic(),
            com.keopsfenks.arcana.skills.components.effects.variables.RemoveVariableMechanic(),
            com.keopsfenks.arcana.skills.components.effects.variables.SetVariableMechanic(),
            com.keopsfenks.arcana.skills.components.effects.visual.ParticleLineMechanic(),
            com.keopsfenks.arcana.skills.components.effects.visual.ParticleMechanic(),
            com.keopsfenks.arcana.skills.components.effects.visual.ParticleRingMechanic(),
            com.keopsfenks.arcana.skills.components.effects.visual.SoundMechanic(),
            com.keopsfenks.arcana.skills.components.effects.visual.StopSoundMechanic(),
            com.keopsfenks.arcana.skills.components.effects.world.BreakBlockMechanic(),
            com.keopsfenks.arcana.skills.components.effects.world.ExplosionMechanic(),
            com.keopsfenks.arcana.skills.components.effects.world.LightningMechanic(),
            com.keopsfenks.arcana.skills.components.effects.world.SetBlockMechanic()
        ))
        add(SkillComponentType.PREDICATE, listOf(
            com.keopsfenks.arcana.skills.components.conditions.ActorTypePredicate(),
            com.keopsfenks.arcana.skills.components.conditions.AllPredicate(),
            com.keopsfenks.arcana.skills.components.conditions.AnyPredicate(),
            com.keopsfenks.arcana.skills.components.conditions.BiomePredicate(),
            com.keopsfenks.arcana.skills.components.conditions.DistancePredicate(),
            com.keopsfenks.arcana.skills.components.conditions.EntityTypePredicate(),
            com.keopsfenks.arcana.skills.components.conditions.EquipmentPredicate(),
            com.keopsfenks.arcana.skills.components.conditions.ExpressionPredicate(),
            com.keopsfenks.arcana.skills.components.conditions.FoodPredicate(),
            com.keopsfenks.arcana.skills.components.conditions.HasAuraPredicate(),
            com.keopsfenks.arcana.skills.components.conditions.HasTargetPredicate(),
            com.keopsfenks.arcana.skills.components.conditions.HealthPredicate(),
            com.keopsfenks.arcana.skills.components.conditions.HeightPredicate(),
            com.keopsfenks.arcana.skills.components.conditions.LineOfSightPredicate(),
            com.keopsfenks.arcana.skills.components.conditions.MovementPredicate(),
            com.keopsfenks.arcana.skills.components.conditions.NotPredicate(),
            com.keopsfenks.arcana.skills.components.conditions.PermissionPredicate(),
            com.keopsfenks.arcana.skills.components.conditions.ResourcePredicate(),
            com.keopsfenks.arcana.skills.components.conditions.StatPredicate(),
            com.keopsfenks.arcana.skills.components.conditions.TimePredicate(),
            com.keopsfenks.arcana.skills.components.conditions.VariablePredicate(),
            com.keopsfenks.arcana.skills.components.conditions.WeatherPredicate(),
            com.keopsfenks.arcana.skills.components.conditions.WorldPredicate()
        ))
        add(SkillComponentType.TARGETER, listOf(
            com.keopsfenks.arcana.skills.components.targeters.BoxTargeter(),
            com.keopsfenks.arcana.skills.components.targeters.ConeTargeter(),
            com.keopsfenks.arcana.skills.components.targeters.EntitiesInRadiusTargeter(),
            com.keopsfenks.arcana.skills.components.targeters.ForwardTargeter(),
            com.keopsfenks.arcana.skills.components.targeters.LineTargeter(),
            com.keopsfenks.arcana.skills.components.targeters.LocationTargeter(),
            com.keopsfenks.arcana.skills.components.targeters.OriginTargeter(),
            com.keopsfenks.arcana.skills.components.targeters.PlayersInRadiusTargeter(),
            com.keopsfenks.arcana.skills.components.targeters.RayTargeter(),
            com.keopsfenks.arcana.skills.components.targeters.SelfTargeter(),
            com.keopsfenks.arcana.skills.components.targeters.TriggerTargeter(),
            com.keopsfenks.arcana.skills.components.targeters.VictimTargeter()
        ))
        add(SkillComponentType.MUTATOR, listOf(
            com.keopsfenks.arcana.skills.components.mutators.CenterLocationMutator(),
            com.keopsfenks.arcana.skills.components.mutators.ClampValueMutator(),
            com.keopsfenks.arcana.skills.components.mutators.MultiplyValueMutator(),
            com.keopsfenks.arcana.skills.components.mutators.OffsetLocationMutator(),
            com.keopsfenks.arcana.skills.components.mutators.RotateLocationMutator(),
            com.keopsfenks.arcana.skills.components.mutators.SetValueMutator(),
            com.keopsfenks.arcana.skills.components.mutators.SwapActorTargetMutator(),
            com.keopsfenks.arcana.skills.components.mutators.TargetAsActorMutator(),
            com.keopsfenks.arcana.skills.components.mutators.TriggerAsTargetMutator()
        ))
        add(SkillComponentType.INTERCEPTOR, listOf(
            com.keopsfenks.arcana.skills.components.interceptors.AddDamageInterceptor(),
            com.keopsfenks.arcana.skills.components.interceptors.CancelEventInterceptor(),
            com.keopsfenks.arcana.skills.components.interceptors.MultiplyDamageInterceptor()
        ))
        add(SkillComponentType.CONTRIBUTION, listOf(
            com.keopsfenks.arcana.skills.components.passives.AttributeContribution(engine.scheduler),
            com.keopsfenks.arcana.skills.components.passives.PotionContribution(potions)
        ))
        add(SkillComponentType.AURA_COMPONENT, listOf(
            com.keopsfenks.arcana.skills.components.auras.AuraAttackComponent(),
            com.keopsfenks.arcana.skills.components.auras.AuraDamagedComponent(),
            com.keopsfenks.arcana.skills.components.auras.AuraStatComponent(),
            com.keopsfenks.arcana.skills.components.auras.AuraTickComponent()
        ))
        add(SkillComponentType.RESOURCE, listOf(HealthResourceProvider(engine.scheduler), FoodResourceProvider(engine.scheduler), ExperienceResourceProvider(engine.scheduler)))
        add(SkillComponentType.STAT, listOf(ContributionStatProvider(platform.contributions)))
        add(SkillComponentType.TRIGGER, listOf(MoveSkillTrigger(owner), JumpSkillTrigger(owner), TimerSkillTrigger(), EquipSkillTrigger(owner), HeldSlotSkillTrigger(owner), ConsumeSkillTrigger(owner), ShootSkillTrigger(owner), KillSkillTrigger(owner), EntityAttackSkillTrigger(owner), EntityDamagedSkillTrigger(owner), CustomSkillTrigger()))
        potions.install(owner)
        platform.interceptorListener.install(owner)
        platform.actorLifecycleListener.install(owner)
    }
}
