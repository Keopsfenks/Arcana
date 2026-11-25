package com.keopsfenks.arcana.skills.platform.auras

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.DefaultSkillEngine
import com.keopsfenks.arcana.skills.execution.ScheduledWork
import com.keopsfenks.arcana.skills.execution.SkillDamageScope
import com.keopsfenks.arcana.skills.platform.DefaultSkillPlatform
import com.keopsfenks.arcana.skills.platform.events.ScopedEventAccess
import com.keopsfenks.arcana.skills.platform.execution.*
import com.keopsfenks.arcana.skills.platform.lifecycle.SkillLifecycleGate
import com.keopsfenks.arcana.skills.platform.providers.StatBindings
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Projectile
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/** Typed, owned subscriptions. Only immutable snapshots cross the MONITOR handoff. */
class AuraEventDispatcher(private val engine: DefaultSkillEngine, private val platform: DefaultSkillPlatform, private val lifecycle: SkillLifecycleGate) : AutoCloseable {
    private class Subscription(val id: UUID, val sequence: Long, val kind: SkillKey, val aura: AuraInstance,
        val lease: com.keopsfenks.arcana.skills.platform.managed.ManagedWorkRegistry.ManagedLease,
        val effects: List<CompiledSkillMechanic>, val interceptors: List<CompiledSkillInterceptor>, val condition: SkillPredicate, val consume: Int) {
        val pending = ConcurrentHashMap.newKeySet<ScheduledWork<*>>()
    }
    private val entries = LinkedHashMap<UUID, Subscription>()
    private val actors = HashMap<UUID, LinkedHashSet<UUID>>()
    private val sequence = AtomicLong()
    private var closed = false
    val subscriptionCount get() = lifecycle.locked { entries.size }
    fun subscribe(kind: SkillKey, aura: AuraInstance, effects: List<CompiledSkillMechanic>, interceptors: List<CompiledSkillInterceptor>, condition: SkillPredicate, consume: Int): AutoCloseable {
        require(consume in 0..1_000_000)
        require(effects.isNotEmpty() || interceptors.isNotEmpty()) { "Aura event subscription requires a callback" }
        if (interceptors.isNotEmpty() && (aura.managed.definition.stats.any { !it.local } || aura.managed.definition.bindings.any { it.persistent })) throw SkillProviderException(SkillResult.of(SkillResult.Status.UNSUPPORTED_CONTEXT, "Local aura interception requires synchronous nonpersistent bindings"))
        require(consume == 0 || interceptors.isNotEmpty()) { "Local charge consumption requires interceptors; normal hooks can use add_aura_charge explicitly" }
        val lease = platform.managed.reserve(aura.definition.revision, aura.actor(), aura.managed.state.root.dependencies, aura.managed.lease.relatedActors)
        val sub = Subscription(lease.id, sequence.incrementAndGet(), kind, aura, lease, java.util.List.copyOf(effects), java.util.List.copyOf(interceptors), condition, consume)
        lease.onClose { lifecycle.locked {
            entries.remove(sub.id)
            actors[aura.actor().id()]?.let { it.remove(sub.id); if (it.isEmpty()) actors.remove(aura.actor().id()) }
        }; sub.pending.toList().forEach { it.cancel() }; sub.pending.clear() }
        val accepted = lifecycle.locked { if (closed || !aura.active() || !lease.active) false else { entries[sub.id] = sub; actors.getOrPut(aura.actor().id()) { LinkedHashSet() }.add(sub.id); true } }
        aura.managed.own(lease)
        if (!accepted) lease.close()
        return lease
    }
    private fun selected(kind: SkillKey, actor: SkillActor) = lifecycle.locked { actors[actor.id()].orEmpty().mapNotNull { entries[it] }.filter { it.kind == kind && it.aura.active() && it.lease.active }.sortedBy { it.sequence } }
    private fun input(sub: Subscription, supplied: SkillInvocation, local: Boolean): SkillInvocation {
        val base = sub.aura.managed.invocation()
        return base.derive().actor(sub.aura.actor()).target(if (local) null else supplied.target().orElse(null))
            .trigger(if (local) null else supplied.trigger().orElse(null))
            .selection(if (local) SkillSelection.empty() else supplied.selection())
            .parameters(base.parameters()).data(supplied.data()).location(null).build()
    }
    fun emit(kind: SkillKey, actor: SkillActor, invocation: SkillInvocation) {
        if (SkillDamageScope.isActive()) return
        for (sub in selected(kind, actor).filter { it.effects.isNotEmpty() }) {
            val snapshot = input(sub, invocation, local = false)
            val work = engine.scheduler.on(actor.entity(), 1) {
                if (sub.lease.active && sub.aura.active()) {
                    val guarded = CompiledSkillMechanic { context ->
                        if (!sub.lease.active || !sub.condition.test(context.invocation())) java.util.concurrent.CompletableFuture.completedFuture(SkillResult.success())
                        else context.execute(sub.effects)
                    }
                    sub.aura.managed.execute(listOf(guarded), sub.aura.values(), snapshot).whenComplete { result, error ->
                        if (sub.aura.active() && sub.lease.active) {
                            val value = if (error == null) result else PlatformExecutionState.failure(error)
                            if (!value.isSuccess) sub.aura.remove(sub.aura.failureReason(value))
                        }
                    }
                }
                Unit
            }
            sub.pending.add(work); work.future.whenComplete { _, _ -> sub.pending.remove(work) }
            sub.aura.managed.state.track(work)
            if (!sub.lease.active || !sub.aura.active()) work.cancel()
        }
    }
    fun intercept(kind: SkillKey, actor: SkillActor, invocation: SkillInvocation, event: Event): List<SkillResult> {
        if (SkillDamageScope.isActive() || !engine.scheduler.owns(actor.entity())) return emptyList()
        val results = mutableListOf<SkillResult>()
        ScopedEventAccess(event).use { access ->
            for (sub in selected(kind, actor).filter { it.interceptors.isNotEmpty() }) {
                if (access.cancelled()) break
                val managed = sub.aura.managed
                var frame: com.keopsfenks.arcana.skills.platform.values.InvocationFrame? = null
                try {
                    managed.callback()
                    val damage = if (access.supportsDamage()) access.damage() else invocation.data()["damage"]?.number() ?: 0.0
                    val supplied = input(sub, invocation, local = true).derive().data(mapOf("damage" to damage) + sub.aura.values()).build()
                    val captured = InvocationSnapshots.captureLocal(supplied, engine.scheduler, managed.definition.snapshots, platform)
                    val enriched = StatBindings.captureLocal(captured, managed.definition.stats, platform)
                    val local = managed.localFrame(enriched); frame = local
                    val variables = platform.variables.access(local)
                    val snapshot = local.snapshot(managed.definition.bindings, variables)
                    if (!sub.condition.test(snapshot)) continue
                    var outcome = SkillResult.success()
                    for ((index, interceptor) in sub.interceptors.withIndex()) {
                        if (index >= 64) throw PlatformBudgetException("Aura interception exceeds 64 local steps")
                        managed.state.step(); local.checkActive()
                        if (!sub.lease.active) throw PlatformDependencyException("Aura event component retired")
                        outcome = interceptor.apply(access, snapshot, managed.eventAdmission())
                        if (!outcome.isSuccess) break
                    }
                    results.add(outcome)
                    if (outcome.isSuccess && sub.aura.active()) sub.aura.consumeLocal(sub.consume, local)
                    else if (!outcome.isSuccess && sub.aura.active()) sub.aura.remove(sub.aura.failureReason(outcome))
                } catch (error: Throwable) {
                    val result = PlatformExecutionState.failure(error); results.add(result)
                    if (sub.aura.active()) sub.aura.remove(sub.aura.failureReason(result))
                } finally { frame?.let { managed.state.release(it) } }
            }
        }
        return java.util.List.copyOf(results)
    }
    private fun counterpart(event: EntityDamageEvent): LivingEntity? {
        val damager = (event as? EntityDamageByEntityEvent)?.damager
        return if (damager is Projectile) damager.shooter as? LivingEntity else damager as? LivingEntity
    }
    private fun generated(event: EntityDamageEvent) = event is EntityDamageByEntityEvent && platform.generatedSources.contains(event.damager.uniqueId)
    fun onHighest(event: EntityDamageEvent) {
        if (SkillDamageScope.isActive() || event.isCancelled || generated(event)) return
        val victim = event.entity as? LivingEntity ?: return
        val actor = SkillActor.of(victim)
        val data = mapOf("damage" to event.damage, "damage_cause" to event.cause.name)
        intercept(SkillKey("arcana", "damaged"), actor, SkillInvocation.builder(actor).data(data).build(), event)
        if (!event.isCancelled && event is EntityDamageByEntityEvent && event.cause in ATTACK_CAUSES) counterpart(event)?.let { attacker ->
            val source = SkillActor.of(attacker)
            intercept(SkillKey("arcana", "attack"), source, SkillInvocation.builder(source).data(data).build(), event)
        }
    }
    fun onMonitor(event: EntityDamageEvent) {
        if (SkillDamageScope.isActive() || event.isCancelled || generated(event) || !event.finalDamage.isFinite()) return
        val victim = event.entity as? LivingEntity ?: return
        val source = counterpart(event)?.let(SkillActor::of)
        val actor = SkillActor.of(victim)
        val data = mapOf("damage" to event.finalDamage, "damage_cause" to event.cause.name)
        emit(SkillKey("arcana", "damaged"), actor, SkillInvocation.builder(actor).target(source).trigger(source).data(data).build())
        if (event is EntityDamageByEntityEvent && event.cause in ATTACK_CAUSES && source != null) emit(SkillKey("arcana", "attack"), source, SkillInvocation.builder(source).target(actor).trigger(actor).data(data).build())
    }
    override fun close() { val all = lifecycle.locked { closed = true; entries.values.toList() }; all.forEach { it.lease.close() } }
    companion object { private val ATTACK_CAUSES = setOf(EntityDamageEvent.DamageCause.ENTITY_ATTACK, EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK, EntityDamageEvent.DamageCause.PROJECTILE) }
}
