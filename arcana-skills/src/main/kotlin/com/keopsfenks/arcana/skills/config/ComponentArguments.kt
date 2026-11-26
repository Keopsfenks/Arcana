package com.keopsfenks.arcana.skills.config

import com.keopsfenks.arcana.core.skills.*
import kotlin.math.floor

object ComponentArguments {
    /** The field list is shared by factory preflight, generated schema and component help. */
    fun describe(id: String, description: String, vararg names: String): com.keopsfenks.arcana.core.skills.platform.SkillDescriptor {
        val booleans = setOf("ambient", "particles", "icon", "external", "unsafe", "physics", "drops", "fire", "break-blocks", "effect-only", "percent", "share-caster", "refresh", "overwrite", "current", "hit-caster", "fail-fast", "normalize", "per-stack", "players-only", "exclude-self")
        val lists = setOf("conditions", "interceptors", "then", "else", "choices", "branches", "types", "biomes", "worlds", "states", "materials", "tags", "components")
        val maps = setOf("condition", "select", "destination", "parameters")
        return com.keopsfenks.arcana.core.skills.platform.SkillDescriptor(id, description, names.distinct().map { name ->
            val type = when (name) { in booleans -> "boolean"; in lists -> "list"; in maps -> "map"; else -> "value" }
            com.keopsfenks.arcana.core.skills.platform.SkillArgumentDescriptor(name, type, false, java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty(), "$name ($type); factory validates values, expressions and conditional requirements")
        })
    }
    fun keys(args: SkillArguments, descriptor: com.keopsfenks.arcana.core.skills.platform.SkillDescriptor) {
        keys(args, *descriptor.arguments().map { it.name() }.toTypedArray())
        for (rule in descriptor.arguments()) if (args.has(rule.name())) {
            val value = args.raw(rule.name())
            val valid = when (rule.type()) { "boolean" -> value is Boolean; "list" -> value is List<*>; "map" -> value is Map<*, *>; else -> true }
            if (!valid) throw args.error(rule.name(), "expected ${rule.type()}")
        }
    }
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
