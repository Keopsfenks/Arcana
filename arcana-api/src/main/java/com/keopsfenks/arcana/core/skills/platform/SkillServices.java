package com.keopsfenks.arcana.core.skills.platform;

import java.util.Map;
import java.util.Objects;

/** Read-only typed runtime service view for one invocation. */
public interface SkillServices {
    <T> T require(Class<T> type);
    static SkillServices of(Map<Class<?>, ?> services) {
        var snapshot = Map.copyOf(services);
        snapshot.forEach((type, value) -> type.cast(value));
        return new SkillServices() {
            @Override public <T> T require(Class<T> type) {
                Object service = snapshot.get(Objects.requireNonNull(type));
                if (service == null) throw new IllegalStateException("Skill service is unavailable: " + type.getName());
                return type.cast(service);
            }
        };
    }
    static SkillServices empty() { return of(Map.of()); }
}
