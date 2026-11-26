package com.keopsfenks.arcana.skills.components.triggers
import com.keopsfenks.arcana.core.ArcanaPlugin
import org.bukkit.event.*
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent
class EquipSkillTrigger(plugin: ArcanaPlugin) : BukkitPlatformTrigger("equip", plugin, setOf("equipment")) {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onEvent(event: EntityEquipmentChangedEvent) {
        val changed = event.equipmentChanges.mapNotNull { (slot, change) ->
            val old = change.oldItem(); val item = change.newItem()
            if (old == item) null else slot.name to mapOf("old_material" to old.type.name, "old_amount" to old.amount, "material" to item.type.name, "amount" to item.amount)
        }.toMap()
        if (changed.isNotEmpty()) emit(event.entity, values = mapOf("equipment" to changed))
    }
}
