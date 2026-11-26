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

class PotionMechanic(private val potions: PotionContributions, private val resolver: (String) -> PotionEffectType? = StatusSupport::potion) : SkillMechanic(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("potion", "Apply a finite owned potion contribution preserving external effects", "target", "potion", "amplifier", "duration", "ambient", "particles", "icon")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val recipient = NativeActions.recipient(arguments); val type = StatusSupport.type(arguments, resolver)
        val amplifier = NativeActions.number(arguments, compilation, "amplifier", 0, 0.0, 255.0, true); val duration = AuraSupport.duration(arguments, compilation, "duration", "5s")
        val ambient = arguments.bool("ambient", false); val particles = arguments.bool("particles", true); val icon = arguments.bool("icon", true)
        val contribution = CompiledSkillContribution { context, identity ->
            val actor = context.invocation().actor(); val handoff = com.keopsfenks.arcana.skills.components.passives.NativeLeaseHandoff()
            val managed = context.services().require(ManagedExecutionState::class.java)
            handoff.finish(context.on(actor) { entity ->
                if (entity.isDead) throw SkillProviderException(SkillResult.of(SkillResult.Status.NO_TARGET, "Potion target is dead"))
                handoff.install(potions.add(actor, identity, type, amplifier.evaluate(context.invocation()).toInt(), ambient, particles, icon, true) { managed.remove(SkillRemovalReason.DISPELLED) }); SkillResult.success()
            })
        }
        return CompiledSkillMechanic { context ->
            val input = context.invocation(); val actor = recipient(input)
            if (actor == null) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.NO_TARGET, "Potion target missing"))
            else if (context !is PlatformExecutionContext) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.UNSUPPORTED_CONTEXT, "Potion requires Arcana execution"))
            else ManagedPulse.start(context, duration.evaluate(input).toLong(), 20, listOf(CompiledSkillMechanic { StatusSupport.owned(it, actor, contribution) }), recipient = actor).thenApply { SkillResult.success() }
        }
    }
}
