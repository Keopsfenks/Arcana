package com.keopsfenks.arcana.skills.config

import com.keopsfenks.arcana.core.skills.*
import kotlin.math.floor

object ComponentArguments {
    fun keys(args: SkillArguments, vararg allowed: String) {
        args.values.keys.firstOrNull { it != "type" && it !in allowed }?.let { throw args.error(it, "unknown option") }
    }

    fun number(args: SkillArguments, key: String, compiler: SkillCompiler, min: Double, max: Double, default: Any? = null, integer: Boolean = false): SkillNumber {
        val value = if (args.has(key)) args.raw(key) else default ?: throw args.error(key, "required numeric value")
        fun validate(number: Double): Double {
            require(number.isFinite() && number in min..max && (!integer || floor(number) == number)) {
                "${args.path}.$key: expected ${if (integer) "an integer" else "a finite number"} in $min..$max"
            }
            return number
        }
        if (value is Number) validate(value.toDouble())
        val compiled = compiler.number(value, "${args.path}.$key")
        return SkillNumber { validate(compiled.evaluate(it)) }
    }

    fun target(args: SkillArguments) = SkillTarget.parse(args.string("target", "caster"))
    fun nonEmpty(args: SkillArguments, key: String): String = args.string(key).also {
        require(it.isNotBlank()) { "${args.path}.$key: must not be blank" }
    }
}
