package com.keopsfenks.arcana.core.skills.platform;
import java.util.Map;
/** Owner-qualified native/stat contribution; close is idempotent and removes only this contribution. */
public interface SkillContributionLease extends AutoCloseable {
    default Map<SkillKey, Double> stats() { return Map.of(); }
    @Override void close();
}
