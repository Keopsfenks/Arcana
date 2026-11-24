package com.keopsfenks.arcana.skills.platform.auras
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.sources.DefinitionRevision
class AuraDefinition(val key: SkillKey, val revision: DefinitionRevision, onStart: List<CompiledSkillMechanic> = emptyList(), onTick: List<CompiledSkillMechanic> = emptyList(), onEnd: List<CompiledSkillMechanic> = emptyList(), tags: Set<SkillKey> = emptySet(), dependencies: Set<SkillRegistration> = emptySet(), components: List<CompiledSkillMechanic> = emptyList()) {
    val components = java.util.List.copyOf(components)
    val onStart = java.util.List.copyOf(onStart); val onTick = java.util.List.copyOf(onTick); val onEnd = java.util.List.copyOf(onEnd)
    val tags = java.util.Set.copyOf(tags); val dependencies = java.util.Set.copyOf(dependencies)
    init { require(tags.size <= 64); require(onStart.size + onTick.size + onEnd.size + components.size <= 1024) }
}
