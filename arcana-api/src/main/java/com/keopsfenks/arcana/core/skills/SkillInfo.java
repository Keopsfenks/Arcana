package com.keopsfenks.arcana.core.skills;

import java.util.List;

/** Immutable administrative view of a skill. */
public record SkillInfo(String id, boolean enabled, List<String> triggers, String source, String problem) {
    public SkillInfo { triggers = List.copyOf(triggers); }
}
