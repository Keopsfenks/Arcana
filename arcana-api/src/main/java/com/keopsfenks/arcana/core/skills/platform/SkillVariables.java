package com.keopsfenks.arcana.core.skills.platform;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/** Invocation-bound variable access. Persistent reads are prefetched by the execution engine. */
public interface SkillVariables {
    Optional<SkillValue> get(SkillVariableScope scope, SkillKey key);
    /** Zero TTL means the scope's lifetime; positive TTL is bounded to 24 hours. */
    CompletionStage<Void> set(SkillVariableScope scope, SkillKey key, SkillValue value, Duration ttl);
    CompletionStage<Double> add(SkillVariableScope scope, SkillKey key, double delta);
    CompletionStage<Boolean> remove(SkillVariableScope scope, SkillKey key);
}
