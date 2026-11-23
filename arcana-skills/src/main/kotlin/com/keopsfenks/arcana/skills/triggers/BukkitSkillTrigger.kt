package com.keopsfenks.arcana.skills.triggers

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.scheduling.ArcanaTask
import com.keopsfenks.arcana.core.skills.SkillContext
import com.keopsfenks.arcana.core.skills.SkillTrigger
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.Event
import org.bukkit.event.Cancellable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

/** Shared listener lifecycle and one-tick handoff; each concrete trigger owns one event type. */
abstract class BukkitSkillTrigger(id: String, private val plugin: ArcanaPlugin) : SkillTrigger(id), Listener {
    private val dispatcher = AtomicReference<Consumer<SkillContext>?>()
    private val pending = ConcurrentHashMap.newKeySet<ArcanaTask>()

    protected fun cancelled(event: Event): Boolean = event is Cancellable && event.isCancelled

    override fun listen(fire: Consumer<SkillContext>): AutoCloseable {
        check(dispatcher.compareAndSet(null, fire)) { "Trigger $id is already listening" }
        try { plugin.server.pluginManager.registerEvents(this, plugin) }
        catch (error: Throwable) { dispatcher.compareAndSet(fire, null); throw error }
        val active = AtomicBoolean(true)
        return AutoCloseable {
            if (active.compareAndSet(true, false)) {
                dispatcher.compareAndSet(fire, null)
                HandlerList.unregisterAll(this)
                pending.toList().forEach { it.cancel() }
                pending.clear()
            }
        }
    }

    protected fun emit(context: SkillContext) {
        val fire = dispatcher.get() ?: return
        val dispatch = if (fire is SkillTriggerDispatch) fire.prepare(context) ?: return else Runnable { fire.accept(context) }
        pending.removeIf { it.isCancelled }
        val handle = AtomicReference<ArcanaTask?>()
        val finished = AtomicBoolean(false)
        fun complete() { finished.set(true); handle.get()?.let { pending.remove(it) } }
        try {
            val task = plugin.scheduler.on(context.caster).onRetired { complete() }.run {
                try { if (dispatcher.get() === fire) dispatch.run() }
                finally { complete() }
            }
            handle.set(task)
            pending.add(task)
            if (finished.get() || dispatcher.get() !== fire) {
                if (!finished.get()) task.cancel()
                pending.remove(task)
            }
        } catch (error: Exception) { plugin.logger.warning("Could not dispatch skill trigger $id: ${error.message}") }
    }
}
