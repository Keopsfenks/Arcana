package com.keopsfenks.arcana.core.skills.platform;

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/** Emits immutable identity/snapshot invocations. The host owns deferred dispatch and cleanup. */
public abstract class SkillPlatformTrigger implements SkillPlatformComponent {
    private final SkillDescriptor descriptor;
    private final Set<String> dataKeys;
    protected SkillPlatformTrigger(SkillDescriptor descriptor, Set<String> dataKeys) {
        this.descriptor = Objects.requireNonNull(descriptor); this.dataKeys = Set.copyOf(dataKeys);
    }
    @Override public final SkillDescriptor descriptor() { return descriptor; }
    public final Set<String> dataKeys() { return dataKeys; }
    public boolean supportsTimerSubscriptions() { return false; }
    public abstract AutoCloseable listen(Consumer<SkillInvocation> consumer);
}
