package com.keopsfenks.arcana.skills.components.triggers
import com.keopsfenks.arcana.core.ArcanaPlugin
import org.bukkit.event.*
import org.bukkit.event.player.*
class MoveSkillTrigger(plugin: ArcanaPlugin) : BukkitPlatformTrigger("move", plugin, setOf("from_x", "from_y", "from_z", "to_x", "to_y", "to_z")) {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEvent(event: PlayerMoveEvent) {
        if (event.isCancelled || event is PlayerTeleportEvent) return
        val from = event.from; val to = event.to ?: return
        if (from.x == to.x && from.y == to.y && from.z == to.z && from.world == to.world) return
        emit(event.player, values = mapOf("from_x" to from.x, "from_y" to from.y, "from_z" to from.z, "to_x" to to.x, "to_y" to to.y, "to_z" to to.z))
    }
}
