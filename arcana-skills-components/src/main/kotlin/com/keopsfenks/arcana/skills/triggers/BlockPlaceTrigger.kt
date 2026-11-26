package com.keopsfenks.arcana.skills.triggers

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.skills.SkillContext
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockPlaceEvent

class BlockPlaceTrigger(plugin: ArcanaPlugin) : BukkitSkillTrigger("block_place", plugin) {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEvent(event: BlockPlaceEvent) {
        if (com.keopsfenks.arcana.skills.execution.SkillWorldMutationScope.isActive()) return
        if (!event.isCancelled) emit(SkillContext.builder(event.player).value("block", event.blockPlaced.type.name).build())
    }
}
