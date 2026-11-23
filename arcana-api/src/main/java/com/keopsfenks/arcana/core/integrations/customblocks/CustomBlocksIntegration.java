package com.keopsfenks.arcana.core.integrations.customblocks;

import com.keopsfenks.arcana.core.integrations.Integration;

/**
 * Wrapper interface for custom block integrations.
 * <p>
 * Implemented for plugins that add custom blocks, such as ItemsAdder, Oraxen, Nexo, and
 * CraftEngine, so that their blocks can be looked up through arcana's own block system.
 *
 * @see CustomBlocksManager
 */
public interface CustomBlocksIntegration extends Integration {
    /**
     * Register all of this plugin's custom blocks into arcana.
     *
     * @see com.keopsfenks.arcana.core.blocks.Blocks
     */
    default void registerAllBlocks() {
        // Override when needed.
    }

    /**
     * Register this plugin's {@link com.keopsfenks.arcana.core.blocks.provider.BlockProvider}s into arcana.
     */
    default void registerProvider() {
        // Override when needed.
    }
}
