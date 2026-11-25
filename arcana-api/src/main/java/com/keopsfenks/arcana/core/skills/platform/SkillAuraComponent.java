package com.keopsfenks.arcana.core.skills.platform;
import com.keopsfenks.arcana.core.skills.SkillArguments;
import java.util.Objects;
/** Separately registered activation factory; its owned subscriptions use the aura's existing budget. */
public abstract class SkillAuraComponent implements SkillPlatformComponent {
    private final SkillDescriptor descriptor;
    protected SkillAuraComponent(SkillDescriptor descriptor) { this.descriptor = Objects.requireNonNull(descriptor); }
    @Override public final SkillDescriptor descriptor() { return descriptor; }
    public abstract CompiledSkillMechanic compile(SkillArguments arguments, SkillCompilation compilation);
}
