package com.keopsfenks.arcana.core.skills.platform;

/** A factory-compiled number with typed snapshot bindings. */
@FunctionalInterface
public interface SkillNumericExpression {
    double evaluate(SkillInvocation invocation);
}
