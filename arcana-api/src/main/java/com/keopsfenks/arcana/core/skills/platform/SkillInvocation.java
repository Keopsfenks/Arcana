package com.keopsfenks.arcana.core.skills.platform;

import java.util.*;

/** Immutable invocation snapshot. Mutable execution frames live behind scoped runtime services. */
public final class SkillInvocation {
    private final SkillActor actor, trigger, target;
    private final SkillLocation origin, location;
    private final SkillSelection selection;
    private final Map<String, SkillValue> data, parameters;

    private SkillInvocation(Builder builder) {
        actor = builder.actor; trigger = builder.trigger; target = builder.target;
        origin = builder.origin; location = builder.location; selection = builder.selection;
        var frozen = SkillValue.of(Map.of("data", builder.data, "parameters", builder.parameters)).map();
        data = frozen.get("data").map(); parameters = frozen.get("parameters").map();
    }
    public static Builder builder(SkillActor actor) { return new Builder(actor); }
    public SkillActor actor() { return actor; }
    public Optional<SkillActor> trigger() { return Optional.ofNullable(trigger); }
    public Optional<SkillActor> target() { return Optional.ofNullable(target); }
    public Optional<SkillLocation> origin() { return Optional.ofNullable(origin); }
    public Optional<SkillLocation> location() { return Optional.ofNullable(location); }
    public SkillSelection selection() { return selection; }
    public Map<String, SkillValue> data() { return data; }
    public Map<String, SkillValue> parameters() { return parameters; }
    public Builder derive() {
        return new Builder(actor).trigger(trigger).target(target).origin(origin).location(location).selection(selection).data(data).parameters(parameters);
    }
    public static final class Builder {
        private SkillActor actor, trigger, target;
        private SkillLocation origin, location;
        private SkillSelection selection = SkillSelection.empty();
        private final Map<String, Object> data = new LinkedHashMap<>(), parameters = new LinkedHashMap<>();
        private Builder(SkillActor actor) { this.actor = Objects.requireNonNull(actor, "actor"); }
        public Builder actor(SkillActor actor) { this.actor = Objects.requireNonNull(actor, "actor"); return this; }
        public Builder trigger(SkillActor trigger) { this.trigger = trigger; return this; }
        public Builder target(SkillActor target) { this.target = target; return this; }
        public Builder origin(SkillLocation origin) { this.origin = origin; return this; }
        public Builder location(SkillLocation location) { this.location = location; return this; }
        public Builder selection(SkillSelection selection) { this.selection = Objects.requireNonNull(selection); return this; }
        public Builder data(Map<String, ?> values) { data.putAll(values); return this; }
        public Builder replaceData(Map<String, ?> values) { data.clear(); data.putAll(values); return this; }
        public Builder parameters(Map<String, ?> values) { parameters.clear(); parameters.putAll(values); return this; }
        public SkillInvocation build() { return new SkillInvocation(this); }
    }
}
