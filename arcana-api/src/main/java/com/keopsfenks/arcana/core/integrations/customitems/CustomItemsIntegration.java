package com.keopsfenks.arcana.core.integrations.customitems;

import com.keopsfenks.arcana.core.integrations.Integration;

/**
 * Wrapper interface for custom item integrations.
 * <p>
 * Implemented for plugins that add custom items, such as ItemsAdder, Oraxen, Nexo,
 * CraftEngine, MythicMobs, ExecutableItems, HeadDatabase, and ItemBridge, so that their
 * items can be looked up through arcana's own item system.
 *
 * @see CustomItemsManager
 */
public interface CustomItemsIntegration extends Integration {
    /**
     * Register all of this plugin's custom items into arcana.
     *
     * @see com.keopsfenks.arcana.core.items.Items
     */
    default void registerAllItems() {
        // Override when needed.
    }

    /**
     * Register this plugin's {@link com.keopsfenks.arcana.core.items.provider.ItemProvider}s into arcana.
     */
    default void registerProvider() {
        // Override when needed.
    }
}
