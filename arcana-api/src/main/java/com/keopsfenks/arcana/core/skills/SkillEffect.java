package com.keopsfenks.arcana.core.skills;

/** Base class for one effect type. Validate configuration in compile, not at each trigger. */
public abstract class SkillEffect extends SkillComponent {
    protected SkillEffect(String id) { super(id); }
    public abstract CompiledSkillEffect compile(SkillArguments arguments, SkillCompiler compiler);
}
