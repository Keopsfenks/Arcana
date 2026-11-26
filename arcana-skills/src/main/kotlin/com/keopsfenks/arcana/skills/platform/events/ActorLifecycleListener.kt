package com.keopsfenks.arcana.skills.platform.events
import com.keopsfenks.arcana.skills.DefaultSkillEngine
import org.bukkit.entity.*
import org.bukkit.event.*
import org.bukkit.event.player.PlayerQuitEvent
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent

/** Retires identity-owned jobs without treating a player's cross-world transfer as logout. */
class ActorLifecycleListener(private val engine: DefaultSkillEngine) : Listener, AutoCloseable {
    private val installed = java.util.concurrent.atomic.AtomicBoolean()
    fun install(plugin: org.bukkit.plugin.Plugin) { check(installed.compareAndSet(false, true)); try { plugin.server.pluginManager.registerEvents(this, plugin) } catch (error: Throwable) { installed.set(false); throw error } }
    @EventHandler(priority = EventPriority.MONITOR)
    fun onQuit(event: PlayerQuitEvent) { engine.cancel(event.player.uniqueId) }
    @EventHandler(priority = EventPriority.MONITOR)
    fun onRemove(event: EntityRemoveFromWorldEvent) {
        val entity = event.entity; val id = entity.uniqueId
        engine.platform().generatedSources.retire(id)
        if (entity is LivingEntity && entity !is Player) engine.cancel(id)
    }
    override fun close() { if (installed.compareAndSet(true, false)) HandlerList.unregisterAll(this) }
}
