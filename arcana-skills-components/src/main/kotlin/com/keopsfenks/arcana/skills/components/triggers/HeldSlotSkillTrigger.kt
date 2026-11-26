package com.keopsfenks.arcana.skills.components.triggers
import com.keopsfenks.arcana.core.ArcanaPlugin
import org.bukkit.event.*
import org.bukkit.event.player.PlayerItemHeldEvent
class HeldSlotSkillTrigger(plugin: ArcanaPlugin) : BukkitPlatformTrigger("held_slot", plugin, setOf("previous_slot", "slot")) {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEvent(event: PlayerItemHeldEvent) { if (!event.isCancelled && event.newSlot != event.previousSlot) emit(event.player, values = mapOf("previous_slot" to event.previousSlot, "slot" to event.newSlot)) }
}
