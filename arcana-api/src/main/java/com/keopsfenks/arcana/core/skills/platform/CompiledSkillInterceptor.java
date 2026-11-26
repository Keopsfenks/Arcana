package com.keopsfenks.arcana.core.skills.platform;
import com.keopsfenks.arcana.core.skills.SkillResult;
@FunctionalInterface
public interface CompiledSkillInterceptor {
    SkillResult apply(SkillEventAccess event, SkillInvocation invocation, SkillAdmission admission);
}
