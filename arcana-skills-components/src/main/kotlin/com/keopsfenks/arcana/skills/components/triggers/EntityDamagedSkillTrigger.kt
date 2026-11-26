package com.keopsfenks.arcana.skills.components.triggers
import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.skills.execution.SkillDamageScope
import org.bukkit.entity.*
import org.bukkit.event.*
import org.bukkit.event.entity.*
class EntityDamagedSkillTrigger(plugin: ArcanaPlugin) : BukkitPlatformTrigger("entity_damaged", plugin, setOf("damage", "cause", "damage_source_id")) {
    override fun supportsInterception() = true

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEvent(event: EntityDamageEvent) {
        if (event.isCancelled || SkillDamageScope.isActive()) return
        val input = com.keopsfenks.arcana.skills.platform.events.DamageEventInputs.capture(event, attack = false, final = true) ?: return
        if (input.actor().entity() is Player) return
        emit(input)
    }
}
