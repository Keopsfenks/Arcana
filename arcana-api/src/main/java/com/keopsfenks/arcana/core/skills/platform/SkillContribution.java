package com.keopsfenks.arcana.core.skills.platform;
import com.keopsfenks.arcana.core.skills.SkillArguments;
import java.util.Objects;
/** Separately registered passive contribution factory. */
public abstract class SkillContribution implements SkillPlatformComponent {
    private final SkillDescriptor descriptor;
    protected SkillContribution(SkillDescriptor descriptor) { this.descriptor = Objects.requireNonNull(descriptor); }
    @Override public final SkillDescriptor descriptor() { return descriptor; }
    public abstract CompiledSkillContribution compile(SkillArguments arguments, SkillCompilation compilation);
}
