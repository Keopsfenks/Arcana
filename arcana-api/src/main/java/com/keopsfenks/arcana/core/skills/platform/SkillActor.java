package com.keopsfenks.arcana.core.skills.platform;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/** Identity/reference only. Read live entity state exclusively on its owning scheduler. */
public final class SkillActor {
    private final UUID id;
    private final LivingEntity entity;
    private SkillActor(LivingEntity entity) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.id = Objects.requireNonNull(entity.getUniqueId(), "entity UUID");
    }
    public static SkillActor of(LivingEntity entity) { return new SkillActor(entity); }
    public UUID id() { return id; }
    public LivingEntity entity() { return entity; }
    public Optional<Player> player() { return entity instanceof Player player ? Optional.of(player) : Optional.empty(); }
    @Override public boolean equals(Object other) { return other instanceof SkillActor actor && id.equals(actor.id); }
    @Override public int hashCode() { return id.hashCode(); }
    @Override public String toString() { return "SkillActor[" + id + "]"; }
}
