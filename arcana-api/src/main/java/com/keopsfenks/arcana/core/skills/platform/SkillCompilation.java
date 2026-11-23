package com.keopsfenks.arcana.core.skills.platform;

import com.keopsfenks.arcana.core.skills.SkillArguments;
import java.util.List;

/** Compilation services shared by separately registered general-actor factories. */
public interface SkillCompilation {
    /** Namespace of the definition being compiled, rather than the built-in component's namespace. */
    default String namespace() { return "arcana"; }
    SkillNumericExpression number(Object value, String path);
    List<CompiledSkillMechanic> effects(List<?> definitions, String path);
    SkillPredicate predicate(SkillArguments definition);
    SkillPredicate predicates(List<?> definitions, String path);
    <T extends SkillPlatformComponent> T component(SkillComponentType<T> type, SkillKey key);
    void referenceSkill(SkillKey key, String path);
}
