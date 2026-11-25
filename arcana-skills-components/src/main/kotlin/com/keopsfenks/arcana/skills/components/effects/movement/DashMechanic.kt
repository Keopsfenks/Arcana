package com.keopsfenks.arcana.skills.components.effects.movement
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.*
import com.keopsfenks.arcana.skills.components.auras.AuraSupport
import com.keopsfenks.arcana.skills.components.effects.projectiles.ProjectileArguments
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.effects.EffectValues
import com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionContext
import com.keopsfenks.arcana.skills.platform.managed.*
import com.keopsfenks.arcana.skills.platform.projectiles.ProjectileMotion
import org.bukkit.util.Vector
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference

class DashMechanic : SkillMechanic(SkillDescriptor.of("dash", "Apply finite owned velocity pulses and release only the last unchanged impulse")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        ComponentArguments.keys(arguments, "target", "direction", "speed", "duration", "period")
        val recipient = NativeActions.recipient(arguments); val direction = ProjectileArguments.direction(arguments, compilation)
        val speed = NativeActions.number(arguments, compilation, "speed", 1, 0.0, 16.0)
        val duration = AuraSupport.duration(arguments, compilation, "duration", "5t"); val period = AuraSupport.duration(arguments, compilation, "period", "1t")
        return CompiledSkillMechanic { context ->
            val input = context.invocation(); val actor = recipient(input)
            if (actor == null) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.NO_TARGET, "Dash target missing"))
            else if (context !is PlatformExecutionContext) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.UNSUPPORTED_CONTEXT, "Dash requires Arcana managed execution"))
            else {
                val velocity = MovementActions.bounded(ProjectileMotion.scale(direction(input), speed.evaluate(input)))
                val last = AtomicReference<Vector?>(); val cleanup = context.services().require(SkillCleanup::class.java)
                val setup = CompiledSkillMechanic { current -> current.services().require(ManagedExecutionState::class.java).own(AutoCloseable {
                    cleanup.on(actor) { entity -> last.get()?.let { if (entity.velocity == it) entity.velocity = Vector() }; SkillResult.success() }
                }); CompletableFuture.completedFuture(SkillResult.success()) }
                val pulse = CompiledSkillMechanic { current -> current.on(actor) { entity -> EffectValues.alive(entity) { entity.velocity = velocity.clone(); last.set(velocity.clone()) } } }
                ManagedPulse.start(context, duration.evaluate(input).toLong(), period.evaluate(input).toLong(), listOf(setup), listOf(pulse), recipient = actor).thenApply { SkillResult.success() }
            }
        }
    }
}
