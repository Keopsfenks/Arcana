package com.keopsfenks.arcana.skills.platform.admission

import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.registry.PlatformRegistration

data class CompiledCost(val provider: PlatformRegistration<SkillResourceProvider>, val amount: SkillNumericExpression, val local: Boolean, val path: String)
data class CompiledCharges(val group: SkillKey, val options: ChargeOptions)
