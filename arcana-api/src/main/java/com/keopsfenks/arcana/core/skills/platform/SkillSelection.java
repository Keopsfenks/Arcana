package com.keopsfenks.arcana.core.skills.platform;

import java.util.*;

/** Deduplicated immutable targets; no live entity state is read. */
public record SkillSelection(List<SkillActor> entities, List<SkillLocation> locations) {
    public SkillSelection {
        entities = List.copyOf(new LinkedHashSet<>(entities));
        locations = List.copyOf(new LinkedHashSet<>(locations));
        if (entities.size() + locations.size() > 256) throw new IllegalArgumentException("Selection exceeds 256 targets");
    }
    public static SkillSelection empty() { return new SkillSelection(List.of(), List.of()); }
}
