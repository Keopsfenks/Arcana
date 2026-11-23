package com.keopsfenks.arcana.core.skills;

import java.util.List;

/** Factory-time access to nested component and numeric expression compilation. */
public interface SkillCompiler {
    SkillNumber number(Object value, String path);
    List<CompiledSkillEffect> effects(List<?> definitions, String path);
    CompiledSkillCondition condition(SkillArguments definition);
    CompiledSkillCondition conditions(List<?> definitions, String path);
    /** Declare a literal skill dependency so missing files can reject the entire reload. */
    void referenceSkill(String skillId, String path);
}
