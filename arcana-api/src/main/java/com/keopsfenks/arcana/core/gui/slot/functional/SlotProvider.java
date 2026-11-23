package com.keopsfenks.arcana.core.gui.slot.functional;

import com.keopsfenks.arcana.core.gui.menu.Menu;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface to run on slot display.
 */
@FunctionalInterface
public interface SlotProvider {
    /**
     * Performs this operation on the given arguments.
     *
     * @param player The player.
     * @param menu   The menu.
     * @return The ItemStack, or null to show an empty slot.
     */
    @Nullable
    ItemStack provide(@NotNull Player player,
                      @NotNull Menu menu);
}
