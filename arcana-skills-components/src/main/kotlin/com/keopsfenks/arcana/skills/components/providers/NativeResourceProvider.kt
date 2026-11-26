package com.keopsfenks.arcana.skills.components.providers

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.execution.SkillScheduler
import com.keopsfenks.arcana.skills.platform.execution.InvocationSnapshots
import org.bukkit.entity.LivingEntity
import java.util.concurrent.atomic.AtomicInteger

/** Local holds are prepared and finalized within one proven actor ownership callback. */
abstract class NativeResourceProvider(
    protected val scheduler: SkillScheduler,
    private val description: SkillDescriptor,
    private val playerOnly: Boolean
) : SkillResourceProvider {
    final override fun descriptor() = description
    final override fun supportsLocal() = true
    final override fun reserve(actor: SkillActor, amount: Double) = scheduler.on(actor.entity()) { reserveLocal(actor, amount) }.future
    final override fun balance(actor: SkillActor) = scheduler.on(actor.entity()) { balanceLocal(actor) }.future
    final override fun reserveLocal(actor: SkillActor, amount: Double): SkillResourceReservation {
        validateAmount(amount)
        ready(actor)?.let { return SkillResourceReservation.denied(it) }
        return try { if (amount == 0.0) held(actor.entity()) {} else hold(actor.entity(), amount) }
        catch (error: SkillProviderException) { SkillResourceReservation.denied(error.result()) }
        catch (error: IllegalArgumentException) { SkillResourceReservation.denied(SkillResult.of(SkillResult.Status.UNSUPPORTED_CONTEXT, "${description.id()} balance is invalid: ${error.message}")) }
    }
    final override fun balanceLocal(actor: SkillActor): Double {
        ready(actor)?.let { throw SkillProviderException(it) }
        return read(actor.entity()).also { require(it.isFinite() && it >= 0) { "${description.id()}: invalid balance" } }
    }
    private fun ready(actor: SkillActor): SkillResult? = when {
        playerOnly && actor.player().isEmpty -> SkillResult.of(SkillResult.Status.UNSUPPORTED_ACTOR, "${description.id()} requires a Player actor")
        !scheduler.owns(actor.entity()) -> SkillResult.of(SkillResult.Status.UNSUPPORTED_CONTEXT, "${description.id()} requires actor ownership")
        !InvocationSnapshots.valid(actor.entity()) -> SkillResult.of(SkillResult.Status.NO_TARGET, "Resource actor is unavailable")
        else -> null
    }
    protected fun insufficient() = SkillResourceReservation.denied(SkillResult.of(SkillResult.Status.INSUFFICIENT_RESOURCE, "Not enough ${description.id()}"))
    protected fun held(actor: LivingEntity, refund: () -> Unit): SkillResourceReservation = object : SkillResourceReservation {
        private val phase = AtomicInteger()
        override fun commit() {
            if (phase.get() == 0) { check(scheduler.owns(actor)) { "Local resource finalization requires actor ownership" }; phase.compareAndSet(0, 1) }
        }
        override fun release() {
            if (phase.get() == 0) {
                check(scheduler.owns(actor)) { "Local resource finalization requires actor ownership" }
                if (phase.compareAndSet(0, 2)) refund()
            }
        }
    }
    protected abstract fun read(actor: LivingEntity): Double
    protected abstract fun hold(actor: LivingEntity, amount: Double): SkillResourceReservation
}
