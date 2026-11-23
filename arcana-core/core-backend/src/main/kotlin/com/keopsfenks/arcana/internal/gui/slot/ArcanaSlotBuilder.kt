package com.keopsfenks.arcana.internal.gui.slot

import com.keopsfenks.arcana.core.gui.slot.Slot
import com.keopsfenks.arcana.core.gui.slot.SlotBuilder
import com.keopsfenks.arcana.core.gui.slot.functional.CaptiveFilter
import com.keopsfenks.arcana.core.gui.slot.functional.SlotHandler
import com.keopsfenks.arcana.core.gui.slot.functional.SlotProvider
import com.keopsfenks.arcana.core.gui.slot.functional.SlotUpdater
import com.keopsfenks.arcana.core.map.listMap
import java.util.function.Predicate
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType

class ArcanaSlotBuilder(private val provider: SlotProvider) : SlotBuilder {
    private var captive = false
    private var captiveFromEmpty = false
    private var updater: SlotUpdater = SlotUpdater { player, menu, _ -> provider.provide(player, menu) }

    private val handlers = listMap<ClickType, SlotHandler>()

    private var captiveFilter =
        CaptiveFilter { _, _, _ -> true }
    private var notCaptiveFor: (Player) -> Boolean = { _ -> false}

    override fun onClick(type: ClickType, action: SlotHandler): SlotBuilder {
        handlers[type].add(action)
        return this
    }

    override fun notCaptiveFor(predicate: Predicate<Player>): SlotBuilder {
        notCaptiveFor = { player -> predicate.test(player) }
        return this
    }

    override fun setCaptiveFilter(filter: CaptiveFilter): SlotBuilder {
        captiveFilter = filter
        return this
    }

    override fun setCaptive(fromEmpty: Boolean): SlotBuilder {
        captive = true
        captiveFromEmpty = fromEmpty
        return this
    }

    override fun setUpdater(updater: SlotUpdater): SlotBuilder {
        this.updater = updater
        return this
    }

    override fun build(): Slot {
        return if (captive) {
            ArcanaCaptiveSlot(
                provider,
                captiveFromEmpty,
                notCaptiveFor,
                captiveFilter
            )
        } else {
            ArcanaSlot(
                provider,
                handlers,
                updater
            )
        }
    }
}
