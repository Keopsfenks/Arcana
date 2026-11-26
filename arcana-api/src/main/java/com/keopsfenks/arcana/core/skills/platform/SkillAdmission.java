package com.keopsfenks.arcana.core.skills.platform;

import java.util.UUID;

/** One engine-issued admission shared by inline work and its already-admitted event continuation. */
public interface SkillAdmission {
    UUID id();
    SkillKey skill();
    SkillActor actor();
    boolean committed();
}
