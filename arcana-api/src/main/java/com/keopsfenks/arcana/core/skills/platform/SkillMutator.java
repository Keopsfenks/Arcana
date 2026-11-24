package com.keopsfenks.arcana.core.skills.platform;

import com.keopsfenks.arcana.core.skills.SkillArguments;
import java.util.Objects;

/** Factory for an immutable child invocation transformation. */
public abstract class SkillMutator implements SkillPlatformComponent {
    private final SkillDescriptor descriptor;
    protected SkillMutator(SkillDescriptor descriptor) { this.descriptor = Objects.requireNonNull(descriptor); }
    @Override public final SkillDescriptor descriptor() { return descriptor; }
    public abstract CompiledSkillMutator compile(SkillArguments arguments, SkillCompilation compilation);
}
