package com.keopsfenks.arcana.skills.platform.legacy

import com.keopsfenks.arcana.core.skills.SkillContext
import com.keopsfenks.arcana.core.skills.SkillResult
import com.keopsfenks.arcana.core.skills.platform.SkillActor
import com.keopsfenks.arcana.core.skills.platform.SkillInvocation

class LegacyActorException(val result: SkillResult) : IllegalArgumentException(result.message())

/** Compatibility boundary: never fabricate a Player caster for a general entity invocation. */
object LegacyInvocationAdapter {
    fun fromLegacy(context: SkillContext): SkillInvocation {
        val builder = SkillInvocation.builder(SkillActor.of(context.caster)).data(context.values.filterValues { it != null })
        context.victim?.let { builder.target(SkillActor.of(it)) }
        return builder.build()
    }

    fun toLegacy(invocation: SkillInvocation): SkillContext {
        val player = invocation.actor().player().orElseThrow {
            LegacyActorException(SkillResult.of(SkillResult.Status.UNSUPPORTED_ACTOR, "Legacy skill components require a Player actor"))
        }
        return SkillContext.builder(player)
            .victim(invocation.target().orElse(null)?.entity())
            .values(invocation.data().mapValues { it.value.plain() })
            .build()
    }
}
