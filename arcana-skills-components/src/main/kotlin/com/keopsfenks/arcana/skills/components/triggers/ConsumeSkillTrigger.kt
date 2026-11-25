package com.keopsfenks.arcana.skills.components.triggers
import com.keopsfenks.arcana.core.ArcanaPlugin
import org.bukkit.event.*
import org.bukkit.event.player.PlayerItemConsumeEvent
class ConsumeSkillTrigger(plugin: ArcanaPlugin) : BukkitPlatformTrigger("consume", plugin, setOf("hand", "material", "amount")) {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEvent(event: PlayerItemConsumeEvent) { if (!event.isCancelled) { val item = event.item; emit(event.player, values = mapOf("hand" to event.hand.name, "material" to item.type.name, "amount" to item.amount)) } }
}
