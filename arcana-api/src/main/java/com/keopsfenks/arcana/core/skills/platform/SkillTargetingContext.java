package com.keopsfenks.arcana.core.skills.platform;

import java.util.Objects;
import java.util.function.DoubleSupplier;

/** Selection uses immutable snapshots, owner-context queries and an injected reproducible random source. */
public record SkillTargetingContext(SkillInvocation invocation, SkillTargetQuery query, DoubleSupplier random) {
    public SkillTargetingContext { Objects.requireNonNull(invocation); Objects.requireNonNull(query); Objects.requireNonNull(random); }
}
