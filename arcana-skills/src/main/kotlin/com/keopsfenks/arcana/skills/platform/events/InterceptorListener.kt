package com.keopsfenks.arcana.skills.platform.events

import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.DefaultSkillEngine
import com.keopsfenks.arcana.skills.execution.ScheduledWork
import com.keopsfenks.arcana.skills.execution.SkillDamageScope
import com.keopsfenks.arcana.skills.platform.DefaultSkillPlatform
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.*
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.plugin.Plugin
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/** HIGHEST mutates synchronously; MONITOR hands immutable continuations to entity scheduling.
 * The pending table stores only opaque identity numbers and snapshots, never Events.
 * An identity-number collision discards both batches rather than dispatching the wrong one.
 */
class InterceptorListener(private val engine: DefaultSkillEngine, private val platform: DefaultSkillPlatform) : Listener, AutoCloseable {
    private data class EventKey(val thread: Long, val identity: Int)
    private class Batch(val continuations: MutableList<AdmittedContinuation>) { var expiry: ScheduledWork<Unit>? = null }
    private val pending = ConcurrentHashMap<EventKey, Batch>()
    private val closed = AtomicBoolean()
    private val installed = AtomicBoolean()
    val pendingCount: Int get() = pending.size
    fun install(plugin: Plugin) { check(installed.compareAndSet(false, true)); try { plugin.server.pluginManager.registerEvents(this, plugin) } catch (error: Throwable) { installed.set(false); throw error } }
    private fun key(event: Event) = EventKey(Thread.currentThread().threadId(), System.identityHashCode(event))
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onHighest(event: EntityDamageEvent) {
        if (closed.get() || event.isCancelled || SkillDamageScope.isActive()) return
        val victim = event.entity as? LivingEntity ?: return
        val damage = event.damage
        if (!damage.isFinite() || damage < 0) return
        val damaged = SkillInvocation.builder(SkillActor.of(victim)).data(mapOf("damage" to damage)).build()
        intercept(SkillKey("arcana", if (victim is Player) "damage_taken" else "entity_damaged"), damaged, event)
        if (event is EntityDamageByEntityEvent && event.cause in setOf(EntityDamageEvent.DamageCause.ENTITY_ATTACK, EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK)) {
            val attacker = event.damager as? LivingEntity ?: return
            val attack = SkillInvocation.builder(SkillActor.of(attacker)).target(SkillActor.of(victim)).data(mapOf("damage" to event.damage)).build()
            intercept(SkillKey("arcana", if (attacker is Player) "melee_attack" else "entity_attack"), attack, event, append = true)
        }
    }
    fun intercept(trigger: SkillKey, invocation: SkillInvocation, event: Event, append: Boolean = false) {
        if (closed.get()) return
        val revisions = platform.repository.snapshot().entries.values.filter { it.enabled && it.native?.let { skill -> trigger in skill.triggers && skill.interceptors.isNotEmpty() } == true }
        if (revisions.isEmpty()) return
        val identity = key(event)
        synchronized(pending) {
            if (closed.get()) return
            if (pending.size >= 1024) return
            if (!append && pending.containsKey(identity)) { pending.remove(identity)?.let(::discard); return }
            val batch = pending[identity] ?: Batch(mutableListOf()).also { pending[identity] = it }
            for (revision in revisions) platform.interceptors.intercept(revision.key, invocation, event).continuation?.let(batch.continuations::add)
            if (batch.continuations.isEmpty()) { pending.remove(identity, batch); return }
            if (batch.expiry == null) {
                batch.expiry = engine.scheduler.on(invocation.actor().entity(), 1) {
                    if (pending.remove(identity, batch)) discard(batch)
                }
                batch.expiry!!.future.whenComplete { _, error -> if (error != null && pending.remove(identity, batch)) discard(batch) }
            }
        }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    fun onMonitor(event: EntityDamageEvent) = observe(event)
    fun observe(event: Event) {
        val batch = synchronized(pending) { pending.remove(key(event)) } ?: return
        batch.expiry?.cancel()
        if (closed.get() || event is Cancellable && event.isCancelled) { discard(batch); return }
        // No closure below captures event; final damage is copied while the callback is active.
        val values = if (event is EntityDamageEvent && event.finalDamage.isFinite()) mapOf("damage" to SkillValue.of(event.finalDamage)) else emptyMap()
        batch.continuations.forEach { continuation ->
            val actor = continuation.actor
            val work = engine.scheduler.on(actor.entity(), 1) { continuation.resume(values); Unit }
            work.future.whenComplete { _, error -> if (error != null) continuation.close() }
        }
    }
    private fun discard(batch: Batch) { batch.expiry?.cancel(); batch.continuations.forEach { it.close() }; batch.continuations.clear() }
    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        if (installed.get()) HandlerList.unregisterAll(this)
        synchronized(pending) { pending.values.toList().forEach(::discard); pending.clear() }
    }
}
