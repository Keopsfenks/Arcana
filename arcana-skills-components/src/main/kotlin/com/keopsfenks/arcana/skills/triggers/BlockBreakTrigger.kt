package com.keopsfenks.arcana.skills.triggers

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.skills.SkillContext
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent

class BlockBreakTrigger(plugin: ArcanaPlugin) : BukkitSkillTrigger("block_break", plugin) {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEvent(event: BlockBreakEvent) {
        if (!event.isCancelled) emit(SkillContext.builder(event.player).value("block", event.block.type.name).build())
    }
}
