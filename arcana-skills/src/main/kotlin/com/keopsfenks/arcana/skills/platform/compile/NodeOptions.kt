package com.keopsfenks.arcana.skills.platform.compile

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.SkillArgumentsReader
import kotlin.math.ceil
import kotlin.math.floor

data class NodeOptions(
    val targeter: CompiledSkillTargeter?, val mutators: List<CompiledSkillMutator>, val conditions: SkillPredicate,
    val chance: SkillNumericExpression, val delay: SkillNumericExpression, val repeat: SkillNumericExpression,
    val interval: SkillNumericExpression, val skipFailure: Boolean, val configured: Boolean
) {
    companion object {
        val OPTIONS = setOf("targeter", "mutators", "conditions", "filters", "chance", "delay", "repeat", "interval", "on-failure")
        fun compile(args: SkillArguments, compiler: SkillCompilation, targeter: (SkillArguments) -> CompiledSkillTargeter, mutator: (SkillArguments) -> CompiledSkillMutator): NodeOptions {
            val target = if (!args.has("targeter")) null else {
                val raw = args.raw("targeter")
                targeter(args.child("targeter", if (raw is String) mapOf("type" to raw) else raw))
            }
            val mutators = args.list("mutators", emptyList<Any>()).mapIndexed { index, raw -> mutator(args.child("mutators[$index]", raw)) }
            val condition = compiler.predicates(args.list("conditions", emptyList<Any>()), "${args.path}.conditions")
            val filter = compiler.predicates(args.list("filters", emptyList<Any>()), "${args.path}.filters")
            val failure = args.string("on-failure", "stop")
            require(failure == "stop" || failure == "skip") { "${args.path}.on-failure: expected stop or skip" }
            return NodeOptions(target, java.util.List.copyOf(mutators), SkillPredicate { condition.test(it) && filter.test(it) },
                number(args, compiler, "chance", 100, 0.0, 100.0), duration(args, compiler, "delay"),
                number(args, compiler, "repeat", 1, 1.0, 100.0, true), duration(args, compiler, "interval"), failure == "skip", args.values.keys.any { it in OPTIONS })
        }
        private fun number(args: SkillArguments, compiler: SkillCompilation, key: String, fallback: Number, min: Double, max: Double, integer: Boolean = false): SkillNumericExpression {
            fun validate(value: Double): Double = value.also { require(it.isFinite() && it in min..max && (!integer || floor(it) == it)) { "${args.path}.$key: expected ${if (integer) "an integer" else "a finite number"} in $min..$max" } }
            val raw = args.raw(key, fallback)
            if (raw is Number) validate(raw.toDouble())
            val compiled = compiler.number(raw, "${args.path}.$key")
            return SkillNumericExpression { validate(compiled.evaluate(it)) }
        }
        private fun duration(args: SkillArguments, compiler: SkillCompilation, key: String): SkillNumericExpression {
            val raw = args.raw(key, "0s")
            if (raw is Number || raw is String && raw.matches(Regex("[0-9]+(?:\\.[0-9]+)?(?:ms|s|m|t)"))) {
                val ticks = SkillArgumentsReader.ticks(raw, "${args.path}.$key")
                return SkillNumericExpression { ticks.toDouble() }
            }
            val seconds = compiler.number(raw, "${args.path}.$key")
            return SkillNumericExpression { invocation -> seconds.evaluate(invocation).also { require(it.isFinite() && it in 0.0..86400.0) { "${args.path}.$key: duration must be between zero and 24 hours" } }.let { ceil(it * 20) } }
        }
    }
}
