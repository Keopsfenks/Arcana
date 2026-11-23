package com.keopsfenks.arcana.core.skills.platform;

import com.keopsfenks.arcana.core.skills.SkillArguments;
import java.util.Map;
import java.util.Objects;

/** Immutable source document, before registered component factories compile it. */
public record SkillDefinition(SkillKey key, Map<String, Object> configuration, String origin) {
    public SkillDefinition {
        Objects.requireNonNull(key); Objects.requireNonNull(origin);
        configuration = new SkillArguments(configuration, origin).getValues();
    }
}
