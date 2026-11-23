package com.keopsfenks.arcana.core.skills;

import java.util.function.Consumer;

/** A trigger type that installs its own listener and returns its cleanup handle. */
public abstract class SkillTrigger extends SkillComponent {
    protected SkillTrigger(String id) { super(id); }
    public abstract AutoCloseable listen(Consumer<SkillContext> fire);
}
