package com.keopsfenks.arcana.core.skills.platform;

import java.util.concurrent.CompletionStage;

@FunctionalInterface
public interface CompiledSkillTargeter {
    CompletionStage<SkillSelection> select(SkillTargetingContext context);
}
