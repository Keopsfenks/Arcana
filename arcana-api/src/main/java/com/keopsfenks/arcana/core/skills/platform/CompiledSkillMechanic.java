package com.keopsfenks.arcana.core.skills.platform;

import com.keopsfenks.arcana.core.skills.SkillResult;
import java.util.concurrent.CompletionStage;

/** Validated effect ready to execute with an owned invocation context. */
@FunctionalInterface
public interface CompiledSkillMechanic {
    CompletionStage<SkillResult> execute(SkillExecutionContext execution);
}
