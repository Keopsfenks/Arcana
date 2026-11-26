package com.keopsfenks.arcana.skills.components.triggers
import com.keopsfenks.arcana.core.ArcanaPlugin
import org.bukkit.event.*
import com.destroystokyo.paper.event.player.PlayerJumpEvent
class JumpSkillTrigger(plugin: ArcanaPlugin) : BukkitPlatformTrigger("jump", plugin, setOf("from_y", "to_y")) {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEvent(event: PlayerJumpEvent) { if (!event.isCancelled && event.to.y > event.from.y) emit(event.player, values = mapOf("from_y" to event.from.y, "to_y" to event.to.y)) }
}
