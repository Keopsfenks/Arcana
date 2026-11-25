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
    /** Returns a reserved immutable data alias; the engine captures only requested fields on their owners. */
    default String snapshot(SkillSnapshotRequest request, String path) { throw new UnsupportedOperationException("Required snapshot compilation is unavailable"); }
    default List<CompiledSkillMechanic> auraComponents(List<?> definitions, String path) { throw new UnsupportedOperationException("Aura component compilation is unavailable"); }
    /** Local callback helpers are explicitly enforced by the native compiler. */
    default List<CompiledSkillInterceptor> interceptors(List<?> definitions, String path) { throw new UnsupportedOperationException("Synchronous interception compilation is unavailable"); }
    default SkillPredicate localPredicates(List<?> definitions, String path) { throw new UnsupportedOperationException("Local predicate compilation is unavailable"); }
    default void requireLocalBindings(String path) { throw new UnsupportedOperationException("Local binding validation is unavailable"); }
    /** Definition-local named mechanic template; group and name remain owner-qualified. */
    default SkillArguments template(SkillKey group, SkillKey name, String path) { throw new IllegalArgumentException(path + ": unknown template " + group + "/" + name); }

}
