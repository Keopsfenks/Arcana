package com.keopsfenks.arcana.skills.components.passives
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.execution.*
import org.bukkit.NamespacedKey
import org.bukkit.entity.LivingEntity
import org.bukkit.event.*
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.plugin.Plugin
import org.bukkit.potion.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/** Owner-thread reconciliation; external effects are never overwritten or restored from snapshots. */
class PotionContributions(private val scheduler: SkillScheduler) : Listener, AutoCloseable {
    private data class Key(val actor: UUID, val potion: NamespacedKey)
    private data class Claim(val id: SkillKey, val effect: PotionEffect, val dispellable: Boolean, val onDispel: () -> Unit)
    private class Group(val actor: SkillActor, val type: PotionEffectType) {
        val claims = LinkedHashMap<SkillKey, Claim>()
        var owned: PotionEffect? = null
        var task: ScheduledWork<Unit>? = null
    }
    private val groups = ConcurrentHashMap<Key, Group>()
    private val writing = ThreadLocal.withInitial { mutableSetOf<Key>() }
    private val closed = AtomicBoolean()
    private val installed = AtomicBoolean()
    fun install(plugin: Plugin) { check(installed.compareAndSet(false, true)); try { plugin.server.pluginManager.registerEvents(this, plugin) } catch (error: Throwable) { installed.set(false); throw error } }
    fun add(actor: SkillActor, identity: SkillKey, type: PotionEffectType, amplifier: Int, ambient: Boolean, particles: Boolean, icon: Boolean): SkillContributionLease {
        return add(actor, identity, type, amplifier, ambient, particles, icon, false) {}
    }
    fun add(actor: SkillActor, identity: SkillKey, type: PotionEffectType, amplifier: Int, ambient: Boolean, particles: Boolean, icon: Boolean, dispellable: Boolean, onDispel: () -> Unit): SkillContributionLease {
        check(scheduler.owns(actor.entity())) { "Potion contribution requires actor ownership" }; check(!closed.get())
        val key = Key(actor.id(), type.key)
        val group = groups.computeIfAbsent(key) { Group(actor, type) }
        val claim = Claim(identity, PotionEffect(type, 40, amplifier, ambient, particles, icon), dispellable, onDispel)
        check(group.claims.putIfAbsent(identity, claim) == null) { "Duplicate potion contribution identity" }
        val ended = AtomicBoolean()
        val lease = object : SkillContributionLease {
            override fun close() { if (ended.compareAndSet(false, true)) scheduler.on(actor.entity()) {
                if (groups[key] === group && group.claims.remove(identity, claim)) {
                    reconcile(key, group)
                    if (group.claims.isEmpty()) { groups.remove(key, group); group.task?.cancel(); group.task = null }
                }
                Unit
            } }
        }
        try { reconcile(key, group); schedule(key, group); return lease }
        catch (error: Throwable) { lease.close(); throw error }
    }
    private fun matches(current: PotionEffect?, owned: PotionEffect) = current != null && current.type == owned.type && current.amplifier == owned.amplifier && current.isAmbient == owned.isAmbient && current.hasParticles() == owned.hasParticles() && current.hasIcon() == owned.hasIcon() && current.duration in 1..40
    fun removeOwned(actor: SkillActor, type: PotionEffectType) {
        check(scheduler.owns(actor.entity())) { "Potion removal requires actor ownership" }
        val key = Key(actor.id(), type.key); val group = groups[key] ?: return
        val removed = group.claims.values.filter { it.dispellable }
        removed.forEach { group.claims.remove(it.id, it) }
        reconcile(key, group)
        if (group.claims.isEmpty()) { groups.remove(key, group); group.task?.cancel(); group.task = null }
        removed.forEach { it.onDispel() }
    }
    private fun reconcile(key: Key, group: Group) {
        check(scheduler.owns(group.actor.entity()))
        val entity = group.actor.entity()
        val current = entity.getPotionEffect(group.type)
        val owned = group.owned
        if (owned != null && !matches(current, owned)) group.owned = null
        if (current != null && group.owned == null) return // External effect wins until naturally removed.
        val best = group.claims.values.sortedWith(compareByDescending<Claim> { it.effect.amplifier }.thenBy { it.id.toString() }).firstOrNull()?.effect
        writing.get().add(key)
        try {
            if (best == null) {
                if (group.owned != null && matches(current, group.owned!!)) entity.removePotionEffect(group.type)
                group.owned = null
            } else {
                val previous = group.owned
                if (previous != null && previous != best && matches(current, previous)) {
                    entity.removePotionEffect(group.type)
                    // Respect cancellation of the native removal event.
                    if (matches(entity.getPotionEffect(group.type), previous)) return
                    group.owned = null
                }
                if (entity.addPotionEffect(best)) group.owned = best
            }
        } finally { writing.get().remove(key); if (writing.get().isEmpty()) writing.remove() }
    }
    private fun schedule(key: Key, group: Group) {
        if (group.task != null || group.claims.isEmpty() || closed.get()) return
        val work = scheduler.on(group.actor.entity(), 20) {
            group.task = null
            if (groups[key] === group && !closed.get()) {
                if (com.keopsfenks.arcana.skills.platform.execution.InvocationSnapshots.valid(group.actor.entity())) { reconcile(key, group); schedule(key, group) }
                else { groups.remove(key, group); group.claims.clear(); group.owned = null }
            }
            Unit
        }
        group.task = work
        work.future.whenComplete { _, error -> if (error != null) groups.remove(key, group) }
    }
    fun externalChange(actor: SkillActor, type: PotionEffectType) {
        check(scheduler.owns(actor.entity())) { "Potion observation requires actor ownership" }
        val key = Key(actor.id(), type.key)
        if (key !in writing.get()) groups[key]?.owned = null
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onChange(event: EntityPotionEffectEvent) {
        if (event.isCancelled) return
        val entity = event.entity as? LivingEntity ?: return
        if (scheduler.owns(entity)) externalChange(SkillActor.of(entity), event.modifiedType)
    }
    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        if (installed.get()) HandlerList.unregisterAll(this)
        groups.entries.toList().forEach { (key, group) -> scheduler.on(group.actor.entity()) {
            if (groups.remove(key, group)) { group.task?.cancel(); group.task = null; group.claims.clear(); reconcile(key, group) }
            Unit
        } }
    }
}
