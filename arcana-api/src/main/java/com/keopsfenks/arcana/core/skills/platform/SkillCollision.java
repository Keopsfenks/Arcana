package com.keopsfenks.arcana.core.skills.platform;

import java.util.Objects;
import java.util.Optional;

/** Ordered segment contact; empty actor denotes a block collision. */
public record SkillCollision(SkillLocation location, SkillVector normal, Optional<SkillActor> actor, double fraction) {
    public SkillCollision {
        Objects.requireNonNull(location); Objects.requireNonNull(normal); Objects.requireNonNull(actor);
        if (!Double.isFinite(fraction) || fraction < 0 || fraction > 1) throw new IllegalArgumentException("Collision fraction must be in [0,1]");
    }
}
