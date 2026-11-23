package com.keopsfenks.arcana.skills.compile

import java.util.Collections

class SkillSnapshot(skills: Collection<CompiledSkill>) {
    val byId: Map<String, CompiledSkill> = Collections.unmodifiableMap(skills.associateByTo(LinkedHashMap()) { it.id })
    val byTrigger: Map<String, List<CompiledSkill>> = Collections.unmodifiableMap(
        skills.filter { it.enabled }.flatMap { skill -> skill.triggers.map { it to skill } }
            .groupBy({ it.first }, { it.second }).mapValues { Collections.unmodifiableList(it.value) }
    )

    companion object { val EMPTY = SkillSnapshot(emptyList()) }
}
