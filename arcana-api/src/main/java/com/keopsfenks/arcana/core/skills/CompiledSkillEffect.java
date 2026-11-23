package com.keopsfenks.arcana.core.skills;

import java.util.concurrent.CompletionStage;

/** Immutable, prevalidated effect invoked through a bounded execution context. */
@FunctionalInterface
public interface CompiledSkillEffect {
    CompletionStage<SkillResult> execute(SkillExecution execution);
}
