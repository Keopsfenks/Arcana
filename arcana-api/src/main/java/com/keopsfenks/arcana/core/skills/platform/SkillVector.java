package com.keopsfenks.arcana.core.skills.platform;

/** Immutable finite direction or velocity snapshot. */
public record SkillVector(double x, double y, double z) {
    public SkillVector {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) throw new IllegalArgumentException("Skill vector must be finite");
    }
}
