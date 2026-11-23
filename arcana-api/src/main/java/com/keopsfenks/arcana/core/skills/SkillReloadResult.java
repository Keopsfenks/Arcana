package com.keopsfenks.arcana.core.skills;

import java.util.List;

/** Result of staging/committing a complete directory of skill definitions. */
public record SkillReloadResult(boolean success, int loaded, int disabled, List<String> errors) {
    public SkillReloadResult { errors = List.copyOf(errors); }
}
