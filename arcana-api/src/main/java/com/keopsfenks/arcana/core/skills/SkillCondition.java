package com.keopsfenks.arcana.core.skills;

/** Base class for a condition type; predicates use the invocation's copied data. */
public abstract class SkillCondition extends SkillComponent {
    protected SkillCondition(String id) { super(id); }
    public abstract CompiledSkillCondition compile(SkillArguments arguments, SkillCompiler compiler);
}
