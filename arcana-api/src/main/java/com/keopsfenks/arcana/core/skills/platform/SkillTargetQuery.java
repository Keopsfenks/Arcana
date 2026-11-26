package com.keopsfenks.arcana.core.skills.platform;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/** Ownership-safe queries. Unloaded chunks are skipped without loading them. */
public interface SkillTargetQuery {
    /** True only when every chunk in these bounds is loaded. Implementations must never load it. */
    default CompletionStage<Boolean> loaded(SkillBounds bounds) {
        return java.util.concurrent.CompletableFuture.completedFuture(false);
    }
    CompletionStage<List<SkillActor>> entities(SkillBounds bounds);
    CompletionStage<Optional<SkillEntitySnapshot>> snapshot(SkillActor actor);
    /** Optional engine-owned binding projection before a candidate's pure filters run. */
    default CompletionStage<Optional<SkillInvocation>> project(SkillInvocation input, SkillEntitySnapshot candidate) {
        return java.util.concurrent.CompletableFuture.completedFuture(Optional.empty());
    }
    /** Optional projection for a location selection, including missing-target variable defaults. */
    default CompletionStage<Optional<SkillInvocation>> projectLocation(SkillInvocation input, SkillLocation location) {
        return java.util.concurrent.CompletableFuture.completedFuture(Optional.empty());
    }
    CompletionStage<List<SkillCollision>> collisions(SkillLocation from, SkillLocation to, double radius);
}
