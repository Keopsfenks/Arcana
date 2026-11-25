package com.keopsfenks.arcana.skills.platform.events

import com.keopsfenks.arcana.core.skills.SkillRegistration
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.DefaultSkillEngine
import com.keopsfenks.arcana.skills.execution.ScheduledWork
import com.keopsfenks.arcana.skills.platform.DefaultSkillPlatform
import com.keopsfenks.arcana.skills.platform.lifecycle.SkillLifecycleGate
import org.bukkit.plugin.Plugin
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.*

class TriggerSubscriptions(private val engine: DefaultSkillEngine, private val platform: DefaultSkillPlatform, private val lifecycle: SkillLifecycleGate) : AutoCloseable {
    private val listeners = ConcurrentHashMap<SkillRegistration, AutoCloseable>()
    private class Pending(val owner: SkillRegistration, val actor: UUID, val guard: () -> Boolean) { var work: ScheduledWork<Unit>? = null }
    private val pending = ConcurrentHashMap.newKeySet<Pending>()
    private val timers = ConcurrentHashMap.newKeySet<Timer>()
    private val closed = AtomicBoolean()
    val pendingCount get() = pending.size
    fun generated(input: SkillInvocation): Boolean = input.data()["damage_source_id"]?.let { try { platform.generatedSources.contains(UUID.fromString(it.text())) } catch (_: IllegalArgumentException) { false } } ?: false
    fun install(handle: SkillRegistration, key: SkillKey, trigger: SkillPlatformTrigger) {
        val listener = trigger.listen(object : PlatformTriggerDispatch {
            override fun prepare(input: SkillInvocation): Runnable? {
                if (closed.get() || !platform.componentActive(handle) || generated(input)) return null
                val dispatch = platform.prepareTrigger(key, input) ?: return null
                return Runnable { enqueue(handle, input, { true }) { dispatch(); Unit } }
            }
        })
        listeners[handle] = listener
        if (closed.get() || !platform.componentActive(handle)) listeners.remove(handle)?.close()
    }
    private fun enqueue(owner: SkillRegistration, input: SkillInvocation, guard: () -> Boolean, action: () -> Unit) {
        val job = Pending(owner, input.actor().id(), guard)
        synchronized(pending) {
            if (closed.get() || !platform.componentActive(owner) || !guard() || pending.size >= 1024) return
            pending.add(job)
        }
        try {
            val work = engine.scheduler.on(input.actor().entity(), 1) {
                if (pending.remove(job) && !closed.get() && platform.componentActive(owner) && guard()) action()
            }
            job.work = work
            if (!pending.contains(job)) work.cancel()
            work.future.whenComplete { _, _ -> pending.remove(job) }
        } catch (error: Throwable) { pending.remove(job); throw error }
    }
    fun remove(handle: SkillRegistration) {
        try { listeners.remove(handle)?.close() } catch (error: Exception) { engine.reportProviderFailure("Could not close trigger ${handle.id}: ${error.message}") }
        synchronized(pending) { pending.filter { it.owner === handle }.forEach { pending.remove(it); it.work?.cancel() } }
    }
    fun removeActor(id: UUID) { synchronized(pending) { pending.filter { it.actor == id }.forEach { pending.remove(it); it.work?.cancel() } } }
    fun closeOwner(owner: Plugin) { timers.filter { it.token.owner === owner }.forEach { it.remove(SkillRemovalReason.OWNER_UNLOADED) } }
    fun timer(owner: Plugin, key: SkillKey, actor: SkillActor, interval: Long): SkillManagedHandle {
        require(interval in 1..1_728_000) { "Timer interval must be 1..1728000 ticks" }
        val trigger = platform.registry(SkillComponentType.TRIGGER).get(key) ?: throw IllegalArgumentException("Unknown trigger $key")
        require(trigger.component.supportsTimerSubscriptions()) { "$key does not support timer recipients" }
        val token = lifecycle.admit(owner) { object : SkillRegistration {
            private val active = AtomicBoolean(true)
            override fun getId() = key.toString()
            override fun getOwner() = owner
            override fun isActive() = active.get()
            override fun close() { active.set(false) }
        } }
        val lease = try { platform.managed.reserve(token, actor, setOf(trigger)) } catch (error: Throwable) { token.close(); throw error }
        val timer = Timer(token, key, actor, interval, lease)
        timers.add(timer); lease.onClose { timer.stop() }
        if (!lease.active || closed.get()) timer.remove(SkillRemovalReason.OWNER_UNLOADED) else timer.schedule()
        return timer
    }
    inner class Timer(val token: SkillRegistration, private val key: SkillKey, private val subject: SkillActor, private val interval: Long,
                      private val lease: com.keopsfenks.arcana.skills.platform.managed.ManagedWorkRegistry.ManagedLease) : SkillManagedHandle {
        private val revisionId = UUID.randomUUID(); private val jobs = ConcurrentHashMap.newKeySet<ScheduledWork<Unit>>()
        override fun id() = lease.id
        override fun revision() = revisionId
        override fun skill() = key
        override fun actor() = subject
        override fun active() = lease.active && !closed.get()
        override fun reason() = Optional.ofNullable(lease.reason)
        override fun remove(reason: SkillRemovalReason) { lease.remove(reason) }
        internal fun schedule() {
            if (!active()) return
            val work = try { engine.scheduler.on(subject.entity(), interval) {
                if (active()) {
                    val input = SkillInvocation.builder(subject).build()
                    platform.prepareTrigger(key, input)?.let { dispatch -> enqueue(token, input, ::active) { dispatch(); Unit } }
                    schedule()
                }
            } } catch (_: Exception) { remove(SkillRemovalReason.CANCELLED); return }
            jobs.add(work); work.future.whenComplete { _, error -> jobs.remove(work); if (error != null) remove(SkillRemovalReason.CANCELLED) }
            if (!active()) work.cancel()
        }
        internal fun stop() { token.close(); this@TriggerSubscriptions.remove(token); jobs.toList().forEach { it.cancel() }; jobs.clear(); timers.remove(this) }
    }
    override fun close() { if (closed.compareAndSet(false, true)) { timers.toList().forEach { it.remove(SkillRemovalReason.ENGINE_STOPPED) }; listeners.keys.toList().forEach(::remove) } }
}
