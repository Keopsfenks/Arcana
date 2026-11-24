package com.keopsfenks.arcana.skills.components.mutators

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.execution.MissingSkillTargetException

internal object MutatorSupport {
    fun number(args: SkillArguments, compiler: SkillCompilation, name: String, fallback: Number): SkillNumericExpression {
        val raw = args.raw(name, fallback)
        if (raw is Number) require(raw.toDouble().isFinite()) { "${args.path}.$name: expected a finite number" }
        val compiled = compiler.number(raw, "${args.path}.$name")
        return SkillNumericExpression { compiled.evaluate(it).also { value -> require(value.isFinite()) { "${args.path}.$name: non-finite result" } } }
    }
    fun field(args: SkillArguments): String = args.string("field", "location").also { require(it == "location" || it == "origin") { "${args.path}.field: expected location or origin" } }
    fun location(invocation: SkillInvocation, field: String): SkillLocation = (if (field == "origin") invocation.origin() else invocation.location()).orElseThrow { MissingSkillTargetException("$field snapshot is unavailable") }
    fun replace(invocation: SkillInvocation, field: String, value: SkillLocation) = invocation.derive().let { if (field == "origin") it.origin(value) else it.location(value) }.build()
    fun key(args: SkillArguments) = args.string("key").also { require(it.matches(Regex("[a-z][a-z0-9_]{0,100}"))) { "${args.path}.key: invalid data key" } }
    fun numeric(invocation: SkillInvocation, key: String): Double = requireNotNull(invocation.data()[key]) { "Data value $key is unavailable" }.number()
}
