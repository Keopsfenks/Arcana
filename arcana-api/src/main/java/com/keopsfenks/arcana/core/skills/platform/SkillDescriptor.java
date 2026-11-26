package com.keopsfenks.arcana.core.skills.platform;

import java.util.*;

/** Immutable component metadata; a registry supplies its owner-qualified key. */
public record SkillDescriptor(String id, String description, List<SkillArgumentDescriptor> arguments) {
    public SkillDescriptor {
        Objects.requireNonNull(id); Objects.requireNonNull(description);
        arguments = List.copyOf(arguments);
        new SkillKey("arcana", id);
        if (arguments.stream().map(SkillArgumentDescriptor::name).distinct().count() != arguments.size()) throw new IllegalArgumentException("Duplicate argument descriptor");
    }
    public static SkillDescriptor of(String id, String description) { return new SkillDescriptor(id, description, List.of()); }
}
