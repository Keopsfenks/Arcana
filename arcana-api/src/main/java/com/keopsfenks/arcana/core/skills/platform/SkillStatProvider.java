package com.keopsfenks.arcana.core.skills.platform;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;

/** Supplies finite immutable numeric snapshots for the requested namespaced stat keys. */
public interface SkillStatProvider extends SkillPlatformComponent {
    CompletionStage<Map<SkillKey, Double>> snapshot(SkillActor actor, Set<SkillKey> keys);
    default boolean supportsLocal() { return false; }
    default Map<SkillKey, Double> snapshotLocal(SkillActor actor, Set<SkillKey> keys) {
        throw new UnsupportedOperationException("Stat provider has no synchronous snapshot capability");
    }
}
