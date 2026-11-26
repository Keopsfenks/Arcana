package com.keopsfenks.arcana.core.skills;

import java.util.function.Consumer;

/** A trigger type that installs its own listener and returns its cleanup handle. */
public abstract class SkillTrigger extends SkillComponent {
    protected SkillTrigger(String id) { super(id); }
    /** Whether a host adapter supplies this trigger's synchronous interceptor phase. */
    public boolean supportsInterception() { return false; }
    public abstract AutoCloseable listen(Consumer<SkillContext> fire);
}
