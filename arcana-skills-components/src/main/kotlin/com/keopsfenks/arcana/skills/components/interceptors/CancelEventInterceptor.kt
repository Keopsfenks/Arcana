package com.keopsfenks.arcana.skills.components.interceptors
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
class CancelEventInterceptor : SkillInterceptor(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("cancel_event", "Synchronous event cancellation", "cancelled")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillInterceptor {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val value = arguments.bool("cancelled", true)
        return CompiledSkillInterceptor { event, _, _ ->
            if (!event.supportsCancellation()) SkillResult.of(SkillResult.Status.UNSUPPORTED_CONTEXT, "Event cannot be cancelled")
            else { event.cancelled(value); SkillResult.success() }
        }
    }
}
