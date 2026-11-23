package com.keopsfenks.arcana.internal.spigot.recipes.workstation

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.packet.PacketEvent
import com.keopsfenks.arcana.core.packet.PacketListener
import com.keopsfenks.arcana.core.recipe.workstation.GrindstoneRecipe
import com.keopsfenks.arcana.core.recipe.workstation.WorkstationRecipes
import com.keopsfenks.arcana.internal.spigot.proxies.WorkstationPacketProxy
import org.bukkit.event.inventory.InventoryType

class GrindstonePacketHandler(private val plugin: ArcanaPlugin) : PacketListener {

    override fun onReceive(event: PacketEvent) {
        val slotNum = plugin.getProxy(WorkstationPacketProxy::class.java)
            .getContainerClickSlot(event.packet) ?: return

        if (slotNum != 0 && slotNum != 1) return

        val player = event.player
        if (player.openInventory.topInventory.type != InventoryType.GRINDSTONE) return

        val cursor = player.itemOnCursor
        if (cursor == null || cursor.type.isAir) return

        val matches = WorkstationRecipes.getAll(GrindstoneRecipe::class.java)
            .any { recipe ->
                when (slotNum) {
                    0 -> recipe.item1.matches(cursor)
                    1 -> recipe.item2?.matches(cursor) == true
                    else -> false
                }
            }
        if (!matches) return

        event.isCancelled = true

        plugin.scheduler.on(player).run {
            val topInventory = player.openInventory.topInventory
            if (topInventory.type != InventoryType.GRINDSTONE) return@run

            val current = topInventory.getItem(slotNum)
            if (current != null && !current.type.isAir) return@run

            val toPlace = cursor.clone().apply { amount = 1 }
            topInventory.setItem(slotNum, toPlace)
            if (cursor.amount <= 1) player.setItemOnCursor(null)
            else cursor.amount--
            player.updateInventory()
        }
    }
}
