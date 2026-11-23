package com.keopsfenks.arcana.core.skills;

import java.util.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/** Immutable invocation data. Entity references must only be used in their owning scheduler context. */
public final class SkillContext {
    private final Player caster;
    private final LivingEntity victim;
    private final Map<String, Object> values;

    private SkillContext(Builder builder) {
        caster = builder.caster;
        victim = builder.victim;
        values = new SkillArguments(builder.values, "context").getValues();
    }

    public static Builder builder(Player caster) { return new Builder(caster); }
    public Player getCaster() { return caster; }
    public UUID getCasterId() { return caster.getUniqueId(); }
    public @Nullable LivingEntity getVictim() { return victim; }
    public Map<String, Object> getValues() { return values; }
    public double getNumber(String key) {
        Object value = values.get(key);
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }
    public String getText(String key) { return Objects.toString(values.get(key), ""); }
    public boolean getFlag(String key) { return Boolean.TRUE.equals(values.get(key)); }
    public Builder toBuilder() { return new Builder(caster).victim(victim).values(values); }

    public static final class Builder {
        private final Player caster;
        private LivingEntity victim;
        private final Map<String, Object> values = new LinkedHashMap<>();
        private Builder(Player caster) { this.caster = Objects.requireNonNull(caster, "caster"); }
        public Builder victim(@Nullable LivingEntity victim) { this.victim = victim; return this; }
        public Builder value(String name, Object value) { values.put(Objects.requireNonNull(name), value); return this; }
        public Builder values(Map<String, ?> values) { values.forEach(this::value); return this; }
        public SkillContext build() { return new SkillContext(this); }
    }
}
