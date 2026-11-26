package com.keopsfenks.arcana.core.skills.platform;
import java.util.*;
/** Immutable active set supplied by one owner; a slot change replaces only this holder's leases. */
public record SkillHolder(SkillKey key, String slot, List<SkillKey> skills, Map<String, SkillValue> parameters) {
    public SkillHolder {
        Objects.requireNonNull(key); Objects.requireNonNull(slot);
        if (slot.isBlank() || slot.length() > 100) throw new IllegalArgumentException("Holder slot must contain 1..100 characters");
        skills = List.copyOf(skills);
        if (skills.size() > 256 || new HashSet<>(skills).size() != skills.size()) throw new IllegalArgumentException("Holder requires at most 256 distinct skill keys");
        parameters = SkillValue.of(parameters).map();
    }
}
