package com.keopsfenks.arcana.skills.platform.compile

import com.keopsfenks.arcana.core.skills.SkillRegistration
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.values.VariableBinding
import com.keopsfenks.arcana.skills.platform.providers.CompiledStatBinding
import com.keopsfenks.arcana.skills.platform.admission.CompiledCost
import com.keopsfenks.arcana.skills.platform.admission.CompiledCharges

/** Immutable compiled definition, including captured provider handles and admission options. */
data class CompiledPlatformSkill(
    val key: SkillKey,
    val enabled: Boolean,
    val triggers: List<SkillKey>,
    val permission: String?,
    val chance: Double,
    val cooldownNanos: Long,
    val cooldownGroup: SkillKey,
    val conditions: SkillPredicate,
    val effects: List<CompiledSkillMechanic>,
    val dependencies: Set<SkillRegistration>,
    val references: Map<SkillKey, String>,
    val origin: String,
    val parameters: List<SkillParameter> = emptyList(),
    val bindings: List<VariableBinding> = emptyList(),
    val costs: List<CompiledCost> = emptyList(),
    val charges: CompiledCharges? = null,
    val every: Int = 1,
    val stats: List<CompiledStatBinding> = emptyList()
)
