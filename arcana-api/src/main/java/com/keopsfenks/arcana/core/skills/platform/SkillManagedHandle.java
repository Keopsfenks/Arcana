package com.keopsfenks.arcana.core.skills.platform;
import java.util.UUID;
import java.util.Optional;
/** Owned aura/projectile lifetime, independent of normal completion of its creating skill. */
public interface SkillManagedHandle extends AutoCloseable {
    UUID id();
    SkillKey skill();
    UUID revision();
    SkillActor actor();
    boolean active();
    Optional<SkillRemovalReason> reason();
    void remove(SkillRemovalReason reason);
    @Override default void close() { remove(SkillRemovalReason.CANCELLED); }
}
