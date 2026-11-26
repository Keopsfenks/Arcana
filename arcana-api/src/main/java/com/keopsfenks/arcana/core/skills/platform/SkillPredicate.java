package com.keopsfenks.arcana.core.skills.platform;

/** Predicates use immutable invocation data, never unscheduled live Bukkit state. */
@FunctionalInterface
public interface SkillPredicate {
    boolean test(SkillInvocation invocation);
}
