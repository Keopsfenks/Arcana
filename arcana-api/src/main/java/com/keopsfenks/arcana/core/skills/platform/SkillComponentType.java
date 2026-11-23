package com.keopsfenks.arcana.core.skills.platform;

import java.util.Objects;

/** Type token for independent registries; registration needs no component-kind switch. */
public record SkillComponentType<T extends SkillPlatformComponent>(String id, Class<T> componentClass) {
    public static final SkillComponentType<SkillMechanic> MECHANIC = new SkillComponentType<>("mechanic", SkillMechanic.class);
    public static final SkillComponentType<SkillPredicateType> PREDICATE = new SkillComponentType<>("predicate", SkillPredicateType.class);
    public static final SkillComponentType<SkillTargeter> TARGETER = new SkillComponentType<>("targeter", SkillTargeter.class);
    public SkillComponentType {
        Objects.requireNonNull(componentClass);
        new SkillKey("arcana", id);
    }
}
