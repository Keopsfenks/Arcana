package com.keopsfenks.arcana.core.skills.platform;
import java.util.concurrent.CompletionStage;
@FunctionalInterface
public interface CompiledSkillContribution {
    /** Identity is unique to this holder/definition/owner lease; do not reuse another owner's modifier. */
    CompletionStage<SkillContributionLease> apply(SkillExecutionContext context, SkillKey identity);
}
