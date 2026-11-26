package com.keopsfenks.arcana.skills.platform.projectiles
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.core.skills.SkillRegistration

class ProjectileHooks(
    onStart: List<CompiledSkillMechanic> = emptyList(), onTick: List<CompiledSkillMechanic> = emptyList(),
    onHit: List<CompiledSkillMechanic> = emptyList(), onEnd: List<CompiledSkillMechanic> = emptyList(),
    val hitCondition: SkillPredicate = SkillPredicate { true }, dependencies: Set<SkillRegistration> = emptySet()
) {
    val onStart = java.util.List.copyOf(onStart); val onTick = java.util.List.copyOf(onTick)
    val onHit = java.util.List.copyOf(onHit); val onEnd = java.util.List.copyOf(onEnd)
    val dependencies = java.util.Set.copyOf(dependencies)
}
