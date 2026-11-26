package com.keopsfenks.arcana.core.skills.platform;

import java.util.Objects;
import java.util.Optional;

/** Shared configuration/help metadata; factories validate the same constraints. */
public record SkillArgumentDescriptor(String name, String type, boolean required, Optional<SkillValue> defaultValue,
                                      Optional<Double> minimum, Optional<Double> maximum, String description) {
    public SkillArgumentDescriptor {
        Objects.requireNonNull(name); Objects.requireNonNull(type); Objects.requireNonNull(defaultValue);
        Objects.requireNonNull(minimum); Objects.requireNonNull(maximum); Objects.requireNonNull(description);
        if (name.isBlank() || type.isBlank()) throw new IllegalArgumentException("Argument name/type must not be blank");
        if (minimum.filter(value -> !Double.isFinite(value)).isPresent() || maximum.filter(value -> !Double.isFinite(value)).isPresent()
                || (minimum.isPresent() && maximum.isPresent() && minimum.get() > maximum.get())) throw new IllegalArgumentException("Invalid argument range");
        if (required && defaultValue.isPresent()) throw new IllegalArgumentException("Required argument cannot have a default");
    }
}
