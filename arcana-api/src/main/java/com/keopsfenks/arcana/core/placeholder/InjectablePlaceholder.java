package com.keopsfenks.arcana.core.placeholder;

import com.keopsfenks.arcana.core.ArcanaPlugin;
import org.jetbrains.annotations.Nullable;

/**
 * Placeholders that can be injected into {@link PlaceholderInjectable} objects.
 */
public interface InjectablePlaceholder extends Placeholder {
    /**
     * Get the plugin that owns the placeholder.
     *
     * @return Always null, as injectable placeholders are not owned by a plugin.
     */
    @Nullable
    @Override
    default ArcanaPlugin getPlugin() {
        return null;
    }
}
