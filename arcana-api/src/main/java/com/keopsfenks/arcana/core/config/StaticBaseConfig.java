package com.keopsfenks.arcana.core.config;

import com.keopsfenks.arcana.core.Arcana;
import com.keopsfenks.arcana.core.PluginLike;
import com.keopsfenks.arcana.core.config.wrapper.LoadableConfigWrapper;
import org.jetbrains.annotations.NotNull;

/**
 * Non-updatable config that exists within a plugin jar.
 */
public abstract class StaticBaseConfig extends LoadableConfigWrapper {
    /**
     * Create a new static base config, for configs present in the plugin's base
     * directory (eg config.yml, lang.yml).
     * <p>
     * Does not automatically update.
     *
     * @param configName The name of the config (excluding extension).
     * @param plugin     The plugin.
     * @param type       The config type.
     */
    protected StaticBaseConfig(@NotNull final String configName,
                               @NotNull final PluginLike plugin,
                               @NotNull final ConfigType type) {
        super(Arcana.get().createLoadableConfig(
                configName,
                plugin,
                "",
                plugin.getClass(),
                type,
                true
        ));
    }
}
