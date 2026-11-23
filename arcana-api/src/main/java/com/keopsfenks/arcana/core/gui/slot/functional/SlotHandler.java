package com.keopsfenks.arcana.core.gui.slot.functional;

import com.keopsfenks.arcana.core.gui.menu.Menu;
import com.keopsfenks.arcana.core.gui.slot.Slot;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Interface to run on slot click.
 */
@FunctionalInterface
public interface SlotHandler {
    /**
     * Performs this operation on the given arguments.
     *
     * @param event The click event.
     * @param slot  The slot that was clicked.
     * @param menu  The menu.
     */
    void handle(@NotNull InventoryClickEvent event,
                @NotNull Slot slot,
                @NotNull Menu menu);
}
