package com.keopsfenks.arcana.core.serialization;

import com.keopsfenks.arcana.core.config.interfaces.Config;
import org.jetbrains.annotations.NotNull;

/**
 * Serialize objects to configs.
 *
 * @param <T> The type of object to serialize.
 */
public interface ConfigSerializer<T> {
    /**
     * Serialize an object to a config.
     *
     * @param obj The object.
     * @return The config.
     */
    @NotNull
    Config serialize(@NotNull T obj);
}
