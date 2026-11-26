package com.keopsfenks.arcana.skills.platform.auras
import com.keopsfenks.arcana.core.skills.platform.SkillKey
import java.util.UUID
data class AuraStackKey(val key: SkillKey, val revision: UUID, val target: UUID, val caster: UUID?)
