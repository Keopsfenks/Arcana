package com.keopsfenks.arcana.core.skills.platform;
/** Callback-scoped mutable facade. Every method fails after the interception callback returns. */
public interface SkillEventAccess {
    boolean supportsDamage();
    double damage();
    void damage(double amount);
    boolean supportsCancellation();
    boolean cancelled();
    void cancelled(boolean value);
}
