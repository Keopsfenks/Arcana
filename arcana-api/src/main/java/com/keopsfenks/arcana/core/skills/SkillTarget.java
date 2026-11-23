package com.keopsfenks.arcana.core.skills;

import java.util.Locale;

/** The actor or the living victim provided by an invocation. */
public enum SkillTarget {
    CASTER, VICTIM;

    public static SkillTarget parse(String name) {
        try { return valueOf(name.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException error) { throw new IllegalArgumentException("Unknown target: " + name); }
    }
}
