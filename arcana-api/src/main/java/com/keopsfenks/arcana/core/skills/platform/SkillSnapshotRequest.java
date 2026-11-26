package com.keopsfenks.arcana.core.skills.platform;
import java.util.*;

/** Immutable declaration of extra owned data required by a compiled snapshot predicate. */
public record SkillSnapshotRequest(Kind kind, Role role, String argument, Map<String, SkillValue> options) {
    public enum Kind { PERMISSION, FOOD, MOVEMENT, EQUIPMENT, BIOME, TIME, WEATHER, LINE_OF_SIGHT, RESOURCE, STAT, AURAS }
    public enum Role {
        ACTOR, TARGET, TRIGGER;
        public Optional<SkillActor> reference(SkillInvocation input) {
            return switch (this) { case ACTOR -> Optional.of(input.actor()); case TARGET -> input.target(); case TRIGGER -> input.trigger(); };
        }
        public String prefix() { return name().toLowerCase(Locale.ROOT); }
    }
    public SkillSnapshotRequest {
        Objects.requireNonNull(kind); Objects.requireNonNull(role); Objects.requireNonNull(argument); Objects.requireNonNull(options);
        if (argument.length() > 256 || options.size() > 16) throw new IllegalArgumentException("Snapshot request exceeds argument/options bounds");
        options = SkillValue.of(options).map();
    }
    public static SkillSnapshotRequest of(Kind kind, Role role, String argument) { return new SkillSnapshotRequest(kind, role, argument, Map.of()); }
    public static SkillSnapshotRequest configured(Kind kind, Role role, String argument, Map<String, ?> options) {
        return new SkillSnapshotRequest(kind, role, argument, SkillValue.of(options).map());
    }
}
