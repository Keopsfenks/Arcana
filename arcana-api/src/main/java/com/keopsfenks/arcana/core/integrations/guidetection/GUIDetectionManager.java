package com.keopsfenks.arcana.core.integrations.guidetection;

import com.keopsfenks.arcana.core.integrations.IntegrationRegistry;
import com.keopsfenks.arcana.util.MenuUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Class to handle GUI detection.
 * <p>
 * arcana's own menus are always detected via {@link MenuUtils}; registered
 * {@link GUIDetectionIntegration}s are only consulted if the player does not have an arcana menu open.
 */
public final class GUIDetectionManager {
    /**
     * A set of all registered integrations.
     */
    private static final IntegrationRegistry<GUIDetectionIntegration> REGISTRY = new IntegrationRegistry<>();

    /**
     * Register a new integration.
     *
     * @param integration The integration to register.
     */
    public static void register(@NotNull final GUIDetectionIntegration integration) {
        REGISTRY.register(integration);
    }

    /**
     * Get if a player is in a GUI.
     *
     * @param player The player.
     * @return If the player has an arcana menu open, or any registered integration reports a GUI.
     */
    public static boolean hasGUIOpen(@NotNull final Player player) {
        if (MenuUtils.getOpenMenu(player) != null) {
            return true;
        }

        return REGISTRY.anySafely(integration -> integration.hasGUIOpen(player));
    }

    private GUIDetectionManager() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
