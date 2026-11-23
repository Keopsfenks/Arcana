package com.keopsfenks.arcana.core.particle.impl;

import com.keopsfenks.arcana.core.particle.SpawnableParticle;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Empty (invalid) particle that is spawned when an invalid key is provided.
 */
public final class EmptyParticle implements SpawnableParticle {
    /**
     * Instantiate a new empty particle.
     */
    public EmptyParticle() {

    }

    @Override
    public void spawn(@NotNull final Location location,
                      final int amount) {
        // Do nothing.
    }
}
