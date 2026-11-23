package com.keopsfenks.arcana.core.integrations.customentities;

import com.keopsfenks.arcana.core.integrations.Integration;

/**
 * Wrapper interface for custom entity integrations.
 * <p>
 * Implemented for plugins that add custom entities, such as MythicMobs, so that their
 * entities can be looked up through arcana's own entity system.
 *
 * @see CustomEntitiesManager
 */
public interface CustomEntitiesIntegration extends Integration {
    /**
     * Register all of this plugin's custom entities into arcana.
     *
     * @see com.keopsfenks.arcana.core.entities.Entities
     */
    void registerAllEntities();
}
