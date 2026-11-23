package com.keopsfenks.arcana.core.skills.platform;

import java.util.Objects;
import java.util.UUID;

/** Immutable position; it does not hold or resolve a live Bukkit world. */
public record SkillLocation(UUID world, double x, double y, double z, float yaw, float pitch) {
    public SkillLocation {
        Objects.requireNonNull(world, "world");
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || !Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            throw new IllegalArgumentException("Skill location must be finite");
        }
    }
    public SkillLocation(UUID world, double x, double y, double z) { this(world, x, y, z, 0, 0); }
}
