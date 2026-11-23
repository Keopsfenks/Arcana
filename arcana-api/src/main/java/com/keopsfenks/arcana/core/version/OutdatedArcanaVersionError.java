package com.keopsfenks.arcana.core.version;

import org.jetbrains.annotations.NotNull;

/**
 * An error thrown when arcana is outdated.
 */
public class OutdatedArcanaVersionError extends Error {
    /**
     * Create a new OutdatedArcanaVersionError.
     *
     * @param message The message.
     */
    public OutdatedArcanaVersionError(@NotNull final String message) {
        super(message);
    }
}
