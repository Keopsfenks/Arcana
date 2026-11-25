package com.keopsfenks.arcana.skills.components.effects.status
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.NativeActions
import com.keopsfenks.arcana.skills.components.auras.AuraSupport
import com.keopsfenks.arcana.skills.components.passives.PotionContributions
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionContext
import com.keopsfenks.arcana.skills.platform.managed.ManagedPulse
import com.keopsfenks.arcana.skills.platform.managed.ManagedExecutionState
import org.bukkit.potion.PotionEffectType
import java.util.concurrent.CompletableFuture

class PotionMechanic(private val potions: PotionContributions, private val resolver: (String) -> PotionEffectType? = StatusSupport::potion) : SkillMechanic(SkillDescriptor.of("potion", "Apply a finite owned potion contribution preserving external effects")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        ComponentArguments.keys(arguments, "target", "potion", "amplifier", "duration", "ambient", "particles", "icon")
        val recipient = NativeActions.recipient(arguments); val type = StatusSupport.type(arguments, resolver)
        val amplifier = NativeActions.number(arguments, compilation, "amplifier", 0, 0.0, 255.0, true); val duration = AuraSupport.duration(arguments, compilation, "duration", "5s")
        val ambient = arguments.bool("ambient", false); val particles = arguments.bool("particles", true); val icon = arguments.bool("icon", true)
        val contribution = CompiledSkillContribution { context, identity ->
            val actor = context.invocation().actor(); var native: SkillContributionLease? = null
            val managed = context.services().require(ManagedExecutionState::class.java)
            context.on(actor) { entity ->
                if (entity.isDead) throw SkillProviderException(SkillResult.of(SkillResult.Status.NO_TARGET, "Potion target is dead"))
                native = potions.add(actor, identity, type, amplifier.evaluate(context.invocation()).toInt(), ambient, particles, icon, true) { managed.remove(SkillRemovalReason.DISPELLED) }; SkillResult.success()
            }.thenApply { outcome -> if (!outcome.isSuccess) throw SkillProviderException(outcome); requireNotNull(native) }
        }
        return CompiledSkillMechanic { context ->
            val input = context.invocation(); val actor = recipient(input)
            if (actor == null) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.NO_TARGET, "Potion target missing"))
            else if (context !is PlatformExecutionContext) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.UNSUPPORTED_CONTEXT, "Potion requires Arcana execution"))
            else ManagedPulse.start(context, duration.evaluate(input).toLong(), 20, listOf(CompiledSkillMechanic { StatusSupport.owned(it, actor, contribution) }), recipient = actor).thenApply { SkillResult.success() }
        }
    }
}
