package com.keopsfenks.arcana.internal.gui.slot

import com.keopsfenks.arcana.core.gui.menu.Menu
import com.keopsfenks.arcana.core.gui.slot.Slot
import com.keopsfenks.arcana.core.gui.slot.functional.SlotHandler
import com.keopsfenks.arcana.core.gui.slot.functional.SlotProvider
import com.keopsfenks.arcana.core.gui.slot.functional.SlotUpdater
import com.keopsfenks.arcana.util.openMenu
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

open class ArcanaSlot(
    private val provider: SlotProvider,
    private val handlers: Map<ClickType, List<SlotHandler>>,
    private val updater: SlotUpdater
) : Slot {
    private fun List<SlotHandler>.handle(event: InventoryClickEvent, slot: Slot, menu: Menu) =
        this.forEach { it.handle(event, slot, menu) }

    fun handleInventoryClick(
        event: InventoryClickEvent,
        menu: Menu
    ) {
        handlers[event.click]?.handle(event, this, menu)
    }

    override fun getItemStack(player: Player): ItemStack {
        val menu = player.openMenu ?: return ItemStack(Material.AIR)
        val base = provider.provide(player, menu) ?: return ItemStack(Material.AIR)
        return updater.update(player, menu, base) ?: return ItemStack(Material.AIR)
    }

    override fun getActionableSlot(player: Player, menu: Menu): ArcanaSlot = this

    override fun shouldRenderOnClick(clickType: ClickType) = !handlers[clickType].isNullOrEmpty()
}
