package com.keopsfenks.arcana.core.skills.platform;

import java.util.Objects;
import java.util.Optional;

/** A typed invocation parameter; bound values are immutable after admission. */
public record SkillParameter(String name, SkillValue.Kind type, boolean required, Optional<SkillValue> defaultValue) {
    public SkillParameter {
        new SkillKey("arcana", name);
        Objects.requireNonNull(type); Objects.requireNonNull(defaultValue);
        if (required && defaultValue.isPresent()) throw new IllegalArgumentException("Required parameter cannot have a default: " + name);
        if (defaultValue.isPresent() && defaultValue.get().kind() != type) throw new IllegalArgumentException("Parameter default type differs: " + name);
    }
}
