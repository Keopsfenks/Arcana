package com.keopsfenks.arcana.core.skills.platform;

import java.util.Map;
import java.util.Objects;

/** Owner-context capture used by pure target geometry and predicates. */
public record SkillEntitySnapshot(SkillActor actor, SkillLocation location, SkillBounds bounds, Map<String, SkillValue> data) {
    public SkillEntitySnapshot {
        Objects.requireNonNull(actor); Objects.requireNonNull(location); Objects.requireNonNull(bounds);
        if (!location.world().equals(bounds.world())) throw new IllegalArgumentException("Snapshot worlds differ");
        data = SkillValue.of(data).map();
    }
}
