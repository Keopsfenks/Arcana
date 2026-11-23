package com.keopsfenks.arcana.core.skills.platform;

import com.keopsfenks.arcana.core.skills.SkillArguments;
import java.util.Objects;

/** A general-actor effect factory. One concrete mechanic occupies one class/file. */
public abstract class SkillMechanic implements SkillPlatformComponent {
    private final SkillDescriptor descriptor;
    protected SkillMechanic(SkillDescriptor descriptor) { this.descriptor = Objects.requireNonNull(descriptor); }
    @Override public final SkillDescriptor descriptor() { return descriptor; }
    public abstract CompiledSkillMechanic compile(SkillArguments arguments, SkillCompilation compilation);
}
