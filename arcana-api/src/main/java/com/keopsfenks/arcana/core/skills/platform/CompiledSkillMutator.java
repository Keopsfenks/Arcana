package com.keopsfenks.arcana.core.skills.platform;

@FunctionalInterface
public interface CompiledSkillMutator {
    SkillInvocation mutate(SkillInvocation invocation);
}
