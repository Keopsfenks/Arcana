package com.keopsfenks.arcana.internal.spigot.entities

import com.keopsfenks.arcana.core.Arcana
import com.keopsfenks.arcana.internal.spigot.proxies.SpiderClimbingProxy
import com.keopsfenks.arcana.util.NamespacedKeyUtils
import org.bukkit.entity.Spider
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.EntitiesLoadEvent
import org.bukkit.persistence.PersistentDataType

object SpiderClimbing : Listener {
    val key = NamespacedKeyUtils.createArcanaKey("no_climb")

    fun disable(spider: Spider) {
        if (spider.persistentDataContainer.has(key)) {
            return
        }

        spider.persistentDataContainer.set(key, PersistentDataType.BOOLEAN, true)
        preventClimbing(spider)
    }

    @EventHandler
    fun handleEntitiesLoad(event: EntitiesLoadEvent) {
        event.entities
            .filterIsInstance<Spider>()
            .filter { it.persistentDataContainer.has(key) }
            .forEach { preventClimbing(it) }
    }

    private fun preventClimbing(spider: Spider) {
        val plugin = Arcana.get().arcanaPlugin
        val proxy = plugin.getProxy(SpiderClimbingProxy::class.java)

        proxy.disableWallPathing(spider)

        plugin.scheduler.on(spider).runTimer({ task ->
            if (!spider.isValid) {
                task.cancel()
                return@runTimer
            }

            proxy.stopClimbing(spider)
        }, 1L, 1L)
    }
}
