package com.keopsfenks.arcana.internal.gui.slot

import com.keopsfenks.arcana.core.gui.menu.Menu
import com.keopsfenks.arcana.core.gui.slot.functional.CaptiveFilter
import com.keopsfenks.arcana.core.gui.slot.functional.SlotHandler
import com.keopsfenks.arcana.core.gui.slot.functional.SlotProvider
import com.keopsfenks.arcana.util.toSingletonList
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack

class ArcanaCaptiveSlot(
    provider: SlotProvider,
    private val captiveFromEmpty: Boolean,
    private val notCaptiveFor: (Player) -> Boolean,
    private val filter: CaptiveFilter
) : ArcanaSlot(
    provider,
    ClickType.entries.associateWith {
        captiveWithTest(notCaptiveFor, filter).toSingletonList()
    },
    { _, _, prev -> prev }
) {
    override fun isCaptive(player: Player, menu: Menu): Boolean {
        return !notCaptiveFor(player)
    }

    override fun isAllowedCaptive(player: Player, menu: Menu, itemStack: ItemStack?): Boolean {
        return filter.isAllowed(player, menu, itemStack)
    }

    override fun isCaptiveFromEmpty(): Boolean {
        return captiveFromEmpty
    }
}

private fun captiveWithTest(
    notCaptiveFor: (Player) -> Boolean,
    filter: CaptiveFilter
): SlotHandler = SlotHandler { event, _, menu ->
    val player = event.whoClicked as Player

    val item = event.currentItem.nullIfAir() ?: event.cursor.nullIfAir()

    event.isCancelled = !filter.isAllowed(player, menu, item) || notCaptiveFor(player)
}

private fun ItemStack?.nullIfAir(): ItemStack? =
    if (this?.type == Material.AIR) null else this
