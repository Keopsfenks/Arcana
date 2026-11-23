package com.keopsfenks.arcana.core.skills.platform;

import java.util.Objects;

/** Owner-qualified, read-only published definition metadata. */
public record SkillDefinitionInfo(SkillKey key, SkillKey source, String origin, int schema, boolean enabled, boolean available) {
    public SkillDefinitionInfo { Objects.requireNonNull(key); Objects.requireNonNull(source); Objects.requireNonNull(origin); }
}
