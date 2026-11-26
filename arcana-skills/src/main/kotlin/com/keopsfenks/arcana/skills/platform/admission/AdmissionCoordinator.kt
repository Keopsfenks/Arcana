package com.keopsfenks.arcana.skills.platform.admission

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.DefaultSkillEngine
import com.keopsfenks.arcana.skills.platform.execution.*
import com.keopsfenks.arcana.skills.platform.lifecycle.SkillLifecycleGate
import com.keopsfenks.arcana.skills.platform.sources.DefinitionRevision
import java.util.UUID
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean

class DefaultSkillAdmission internal constructor(val revision: DefinitionRevision, private val actor: SkillActor) : SkillAdmission {
    private val identifier = UUID.randomUUID()
    private val committed = AtomicBoolean()
    override fun id() = identifier
    override fun skill() = revision.key
    override fun actor() = actor
    override fun committed() = committed.get()
    internal fun markCommitted() { committed.set(true) }
}
data class AdmissionOutcome(val admission: DefaultSkillAdmission?, val result: SkillResult)

/** Resource callbacks run outside both the lifecycle gate and actor permit monitors. */
class AdmissionCoordinator(private val engine: DefaultSkillEngine, private val lifecycle: SkillLifecycleGate, clock: () -> Long) {
    private class ActorGate { var active = true }
    private val actors = ConcurrentHashMap<UUID, ActorGate>()
    val charges = ChargeStore(clock)
    val every = EveryCounterStore()
    private val accepted = ConcurrentHashMap<UUID, DefaultSkillAdmission>()
    val activeAdmissions: Int get() = accepted.size

    fun reuse(admission: SkillAdmission, revision: DefinitionRevision, actor: SkillActor): AdmissionOutcome {
        val identifier = admission.id()
        return lifecycle.locked {
            val known = accepted[identifier]
            if (known !== admission || !known.committed() || known.revision !== revision || known.actor().id() != actor.id() ||
                !revision.available() || !lifecycle.available(revision.owner) || revision.dependencies.any { !lifecycle.available(it.owner) }) {
                AdmissionOutcome(null, SkillResult.of(SkillResult.Status.CANCELLED, "Admission token is unavailable or does not match this continuation"))
            } else AdmissionOutcome(known, SkillResult.success())
        }
    }

    fun admit(revision: DefinitionRevision, context: PlatformExecutionContext, local: Boolean = false): CompletableFuture<AdmissionOutcome> {
        val native = requireNotNull(revision.native)
        context.state.checkActive(); context.frame.checkActive()
        if (!every.allow(context.invocation().actor().id(), revision, native.every)) return completed(SkillResult.Status.CONDITION_FAILED, "Every-${native.every} gate did not match")
        val actor = context.invocation().actor()
        val gate = actors.computeIfAbsent(actor.id()) { ActorGate() }
        val resources = ResourceReservations(context, native.costs)
        return resources.prepare().thenCompose {
            val finalize = {
                var cooldown: com.keopsfenks.arcana.skills.execution.CooldownStore.CooldownPermit? = null
                var charge: ChargeStore.ChargePermit? = null
                try {
                    context.state.checkActive(); context.frame.checkActive()
                    if (!InvocationSnapshots.valid(actor.entity())) throw CancellationException("Resource actor retired")
                    resources.enterLocal()
                    val decision = lifecycle.locked { synchronized(gate) {
                        context.state.checkActive(); context.frame.checkActive()
                        if (!gate.active) throw CancellationException("Admission actor retired")
                        if (!revision.available() || revision.dependencies.any { !lifecycle.available(it.owner) }) throw PlatformDependencyException("An admission dependency is unavailable")
                        native.charges?.let { config ->
                            charge = charges.reserve(actor.id(), config.group, config.options)
                            if (charge == null) return@synchronized SkillResult.of(SkillResult.Status.COOLDOWN, "No skill charges available")
                        }
                        cooldown = engine.cooldowns.reserve(actor.id(), native.cooldownGroup.toString(), native.cooldownNanos)
                        if (cooldown == null) return@synchronized SkillResult.of(SkillResult.Status.COOLDOWN, "Skill cooldown is active")
                        if (!resources.decide()) throw CancellationException("Admission cancelled")
                        check(requireNotNull(cooldown).commit()) { "Cooldown permit retired during admission" }
                        check(charge?.commit() != false) { "Charge permit retired during admission" }
                        SkillResult.success()
                    } }
                    if (!decision.isSuccess) {
                        cooldown?.release(); charge?.release(); resources.rollback()
                        AdmissionOutcome(null, decision)
                    } else {
                        resources.commit()
                        val token = DefaultSkillAdmission(revision, actor).also { it.markCommitted() }
                        accepted[token.id()] = token
                        context.frame.onClose { accepted.remove(token.id(), token) }
                        context.state.root.result.whenComplete { _, _ -> accepted.remove(token.id(), token) }
                        AdmissionOutcome(token, SkillResult.success())
                    }
                } catch (error: Throwable) {
                    cooldown?.release(); charge?.release(); resources.rollback()
                    AdmissionOutcome(null, PlatformExecutionState.failure(error))
                }
            }
            if (local) {
                check(engine.scheduler.owns(actor.entity())) { "Local admission requires ownership" }
                CompletableFuture.completedFuture(finalize())
            } else context.track(engine.scheduler.on(actor.entity(), action = finalize))
        }.handle { outcome, error ->
            if (error == null) outcome else { resources.abort(); AdmissionOutcome(null, PlatformExecutionState.failure(error)) }
        }
    }
    internal fun removeActor(id: UUID) {
        val gate = actors.remove(id)
        if (gate != null) synchronized(gate) { gate.active = false; engine.cooldowns.remove(id); charges.remove(id); every.removeActor(id) }
        else { engine.cooldowns.remove(id); charges.remove(id); every.removeActor(id) }
        accepted.entries.removeIf { it.value.actor().id() == id }
    }
    fun remove(handle: SkillRegistration) { every.remove(handle); accepted.entries.removeIf { it.value.revision === handle || handle in it.value.revision.dependencies } }
    fun close() { actors.keys.toList().forEach(::removeActor); accepted.clear(); charges.clear(); every.clear() }
    private fun completed(status: SkillResult.Status, message: String) = CompletableFuture.completedFuture(AdmissionOutcome(null, SkillResult.of(status, message)))
}
