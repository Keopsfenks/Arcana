package com.keopsfenks.arcana.core.skills.platform;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/** Ownership-safe queries. Unloaded chunks are skipped without loading them. */
public interface SkillTargetQuery {
    CompletionStage<List<SkillActor>> entities(SkillBounds bounds);
    CompletionStage<Optional<SkillEntitySnapshot>> snapshot(SkillActor actor);
    CompletionStage<List<SkillCollision>> collisions(SkillLocation from, SkillLocation to, double radius);
}
