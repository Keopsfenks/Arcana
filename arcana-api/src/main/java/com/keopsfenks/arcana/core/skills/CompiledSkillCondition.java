package com.keopsfenks.arcana.core.skills;

/** Tests copied invocation data, on the caster's execution context. */
@FunctionalInterface
public interface CompiledSkillCondition {
    boolean test(SkillContext context);
}
