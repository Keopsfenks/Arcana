package com.keopsfenks.arcana.skills.effects

import com.keopsfenks.arcana.core.skills.SkillResult
import org.bukkit.entity.LivingEntity

internal object EffectValues {
    fun alive(entity: LivingEntity, action: () -> Unit): SkillResult {
        if (entity.isDead) return SkillResult.of(SkillResult.Status.NO_TARGET, "Target is dead")
        action()
        return SkillResult.success()
    }

    fun command(text: String): String = text.removePrefix("/").also {
        require(it.isNotBlank() && '\n' !in it && '\r' !in it) { "Expected one non-empty command line" }
    }
}
