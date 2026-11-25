package com.keopsfenks.arcana.skills.components.conditions
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.NativeActions
import java.util.Locale

internal object ConditionSupport {
    fun subject(args: SkillArguments, key: String = "subject", fallback: String = "actor"): SkillSnapshotRequest.Role = try { SkillSnapshotRequest.Role.valueOf(args.string(key, fallback).uppercase(Locale.ROOT)) } catch (_: IllegalArgumentException) { throw args.error(key, "expected actor, target or trigger") }
    fun data(input: SkillInvocation, role: SkillSnapshotRequest.Role, name: String) = if (role.reference(input).isEmpty) null else input.data()["${role.prefix()}_$name"]
    fun number(value: SkillValue?) = value?.takeIf { it.kind() == SkillValue.Kind.NUMBER }?.number()
    fun location(input: SkillInvocation, role: SkillSnapshotRequest.Role) = data(input, role, "location")?.takeIf { it.kind() == SkillValue.Kind.LOCATION }?.location()
    fun snapshot(args: SkillArguments, compiler: SkillCompilation, kind: SkillSnapshotRequest.Kind, argument: String = "", options: Map<String, Any> = emptyMap()) = compiler.snapshot(SkillSnapshotRequest.configured(kind, subject(args), argument, options), args.path)
    fun strings(args: SkillArguments, key: String, maximum: Int = 256): List<String> = args.list(key).also { require(it.isNotEmpty() && it.size <= maximum) { "${args.path}.$key: expected 1..$maximum entries" } }.map { require(it is String && it.isNotBlank()) { "${args.path}.$key: expected nonempty strings" }; it }
    class Range(args: SkillArguments, compiler: SkillCompilation, lower: Double, upper: Double) {
        private val min = NativeActions.number(args, compiler, "min", lower, lower, upper)
        private val max = NativeActions.number(args, compiler, "max", upper, lower, upper)
        init { val lo = args.raw("min", lower); val hi = args.raw("max", upper); if (lo is Number && hi is Number) require(lo.toDouble() <= hi.toDouble()) { "${args.path}.min: exceeds max" } }
        fun test(value: Double?, input: SkillInvocation): Boolean = value != null && value.isFinite() && value >= min.evaluate(input) && value <= max.evaluate(input)
    }
}
