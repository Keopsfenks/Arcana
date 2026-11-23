package com.keopsfenks.arcana.skills.compile

import com.keopsfenks.arcana.core.skills.*

data class CompiledSkill(
    val id: String,
    val enabled: Boolean,
    val triggers: List<String>,
    val permission: String?,
    val chance: Double,
    val cooldownNanos: Long,
    val cooldownGroup: String,
    val conditions: CompiledSkillCondition,
    val filters: CompiledSkillCondition,
    val effects: List<CompiledSkillEffect>,
    val dependencies: Set<SkillRegistration>,
    val source: String
) {
    fun available() = dependencies.all { it.isActive }
    fun info() = SkillInfo(id, enabled, triggers, source, if (available()) "" else "A component dependency is unavailable")
}
