package com.keopsfenks.arcana.core.skills.platform;

import java.util.List;

/** A plugin-owned definition input. Loading runs outside lifecycle locks and must not mutate files. */
public interface SkillDefinitionSource {
    SkillKey key();
    List<SkillDefinition> load();
}
