package com.keopsfenks.arcana.skills.components.effects.status
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.NativeActions
import com.keopsfenks.arcana.skills.components.auras.AuraSupport
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionContext
import com.keopsfenks.arcana.skills.platform.managed.ManagedPulse
import org.bukkit.NamespacedKey
import org.bukkit.attribute.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

class StunMechanic(private val resolver: (String) -> Attribute? = StatusSupport::attribute) : SkillMechanic(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("stun", "Temporarily suppress walking and supported jumping using owned transient modifiers", "duration", "target")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor()); val recipient = NativeActions.recipient(arguments)
        val duration = AuraSupport.duration(arguments, compilation, "duration", "1s")
        val movement = resolver("movement_speed") ?: throw arguments.error("type", "movement_speed attribute unavailable")
        val jump = resolver("jump_strength")
        val contribution = CompiledSkillContribution { context, identity ->
            val actor = context.invocation().actor(); val added = mutableListOf<Pair<Attribute, AttributeModifier>>()
            val cleanup = context.services().require(SkillCleanup::class.java)
            val handoff = com.keopsfenks.arcana.skills.components.passives.NativeLeaseHandoff()
            val ended = AtomicBoolean()
            val lease = object : SkillContributionLease {
                override fun close() { if (ended.compareAndSet(false, true)) cleanup.on(actor) { entity ->
                    added.forEach { (attribute, modifier) -> entity.getAttribute(attribute)?.let { instance -> if (instance.modifiers.any { it == modifier && it.key == modifier.key }) instance.removeModifier(modifier) } }
                    SkillResult.success()
                } }
            }
            handoff.finish(context.on(actor) { entity ->
                if (entity.isDead) throw SkillProviderException(SkillResult.of(SkillResult.Status.NO_TARGET, "Stun target is dead"))
                val walking = entity.getAttribute(movement) ?: throw SkillProviderException(SkillResult.of(SkillResult.Status.UNSUPPORTED_ACTOR, "Actor has no movement attribute"))
                try {
                    val modifier = AttributeModifier(NamespacedKey(identity.namespace(), identity.value()+"_move"), -1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1)
                    added.add(movement to modifier); walking.addTransientModifier(modifier)
                    jump?.let { attribute -> entity.getAttribute(attribute)?.let { instance ->
                        val jumping = AttributeModifier(NamespacedKey(identity.namespace(), identity.value()+"_jump"), -1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1)
                        added.add(attribute to jumping); instance.addTransientModifier(jumping)
                    } }
                } finally { handoff.install(lease) }
                SkillResult.success()
            })
        }
        return CompiledSkillMechanic { context ->
            val input = context.invocation(); val actor = recipient(input)
            if (actor == null) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.NO_TARGET, "Stun target missing"))
            else if (context !is PlatformExecutionContext) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.UNSUPPORTED_CONTEXT, "Stun requires Arcana execution"))
            else ManagedPulse.start(context, duration.evaluate(input).toLong(), 20, listOf(CompiledSkillMechanic { StatusSupport.owned(it, actor, contribution) }), recipient = actor).thenApply { SkillResult.success() }
        }
    }
}
