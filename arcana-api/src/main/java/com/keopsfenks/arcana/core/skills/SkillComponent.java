package com.keopsfenks.arcana.core.skills;

import java.util.Objects;

/** Common identity for separately registered skill components. */
public abstract class SkillComponent {
    private final String id;

    protected SkillComponent(String id) {
        this.id = Objects.requireNonNull(id, "id");
        if (!id.matches("[a-z0-9_]{1,100}")) {
            throw new IllegalArgumentException("Component ID must match [a-z0-9_]{1,100}: " + id);
        }
    }

    public final String getId() { return id; }
}
