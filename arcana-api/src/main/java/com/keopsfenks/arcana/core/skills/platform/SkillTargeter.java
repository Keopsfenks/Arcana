package com.keopsfenks.arcana.core.skills.platform;

import com.keopsfenks.arcana.core.skills.SkillArguments;
import java.util.Objects;

/** Factory for one bounded entity or location selector. */
public abstract class SkillTargeter implements SkillPlatformComponent {
    private final SkillDescriptor descriptor;
    protected SkillTargeter(SkillDescriptor descriptor) { this.descriptor = Objects.requireNonNull(descriptor); }
    @Override public final SkillDescriptor descriptor() { return descriptor; }
    public abstract CompiledSkillTargeter compile(SkillArguments arguments, SkillCompilation compilation);
}
