package com.keopsfenks.arcana.core.skills.platform;

import com.keopsfenks.arcana.core.skills.SkillArguments;
import java.util.Objects;

/** Factory for an invocation-snapshot condition. */
public abstract class SkillPredicateType implements SkillPlatformComponent {
    private final SkillDescriptor descriptor;
    protected SkillPredicateType(SkillDescriptor descriptor) { this.descriptor = Objects.requireNonNull(descriptor); }
    @Override public final SkillDescriptor descriptor() { return descriptor; }
    /** Explicit opt-in to snapshot-only local interception. */
    public boolean supportsLocal() { return false; }
    public abstract SkillPredicate compile(SkillArguments arguments, SkillCompilation compilation);
}
