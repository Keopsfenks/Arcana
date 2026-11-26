package com.keopsfenks.arcana.core.skills.platform;

import java.util.Objects;
import java.util.UUID;

/** Immutable axis-aligned world-space query or entity bounds. */
public record SkillBounds(UUID world, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
    public SkillBounds {
        Objects.requireNonNull(world, "world");
        for (double value : new double[]{minX, minY, minZ, maxX, maxY, maxZ})
            if (!Double.isFinite(value) || Math.abs(value) > 30_000_000) throw new IllegalArgumentException("Bounds must be finite and within world coordinates");
        if (minX > maxX || minY > maxY || minZ > maxZ) throw new IllegalArgumentException("Reversed query bounds");
    }
    public boolean intersects(SkillBounds other) {
        return world.equals(other.world) && minX <= other.maxX && maxX >= other.minX && minY <= other.maxY && maxY >= other.minY && minZ <= other.maxZ && maxZ >= other.minZ;
    }
}
