package com.keopsfenks.arcana.skills.components.interceptors
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
class CancelEventInterceptor : SkillInterceptor(SkillDescriptor.of("cancel_event", "Synchronous event cancellation")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillInterceptor {
        arguments.values.keys.firstOrNull { it !in setOf("type", "cancelled") }?.let { throw arguments.error(it, "unknown interceptor option") }
        val value = arguments.bool("cancelled", true)
        return CompiledSkillInterceptor { event, _, _ ->
            if (!event.supportsCancellation()) SkillResult.of(SkillResult.Status.UNSUPPORTED_CONTEXT, "Event cannot be cancelled")
            else { event.cancelled(value); SkillResult.success() }
        }
    }
}
