package com.keopsfenks.arcana.skills.components.interceptors
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
class AddDamageInterceptor : SkillInterceptor(SkillDescriptor.of("add_damage", "Synchronous local damage interception")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillInterceptor {
        arguments.values.keys.firstOrNull { it !in setOf("type", "amount") }?.let { throw arguments.error(it, "unknown interceptor option") }
        val raw = arguments.raw("amount", 0)
        if (raw is Number) require(raw.toDouble().isFinite() && raw.toDouble() in 0.0..1_000_000.0) { "${arguments.path}.amount: expected a number between 0 and 1000000" }
        val value = compilation.number(raw, "${arguments.path}.amount")
        return CompiledSkillInterceptor { event, invocation, _ ->
            if (!event.supportsDamage()) SkillResult.of(SkillResult.Status.UNSUPPORTED_CONTEXT, "Event has no damage")
            else {
                val amount = value.evaluate(invocation)
                require(amount.isFinite() && amount in 0.0..1_000_000.0) { "${arguments.path}.amount: invalid evaluated value" }
                event.damage((event.damage() + amount).coerceIn(0.0, 1_000_000.0)); SkillResult.success()
            }
        }
    }
}
