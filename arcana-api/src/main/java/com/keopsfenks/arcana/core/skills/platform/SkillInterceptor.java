package com.keopsfenks.arcana.core.skills.platform;
import com.keopsfenks.arcana.core.skills.SkillArguments;
import java.util.Objects;
/** Synchronous local operation; cannot schedule work or retain the event facade. */
public abstract class SkillInterceptor implements SkillPlatformComponent {
    private final SkillDescriptor descriptor;
    protected SkillInterceptor(SkillDescriptor descriptor) { this.descriptor = Objects.requireNonNull(descriptor); }
    @Override public final SkillDescriptor descriptor() { return descriptor; }
    public abstract CompiledSkillInterceptor compile(SkillArguments arguments, SkillCompilation compilation);
}
