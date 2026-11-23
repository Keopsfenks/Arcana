package com.keopsfenks.arcana.core.skills;

/** A numeric constant or expression compiled once for a definition. */
@FunctionalInterface
public interface SkillNumber {
    double evaluate(SkillContext context);
}
