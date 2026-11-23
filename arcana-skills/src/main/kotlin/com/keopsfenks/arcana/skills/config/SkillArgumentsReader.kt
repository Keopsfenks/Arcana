package com.keopsfenks.arcana.skills.config

import kotlin.math.ceil

object SkillArgumentsReader {
    private const val MAX_NANOS = 86_400_000_000_000L
    private val durationPattern = Regex("([0-9]+(?:\\.[0-9]+)?)(ms|s|m|t)")

    fun durationNanos(value: Any?, path: String): Long {
        val nanos = when (value) {
            is Number -> value.toDouble() * 1_000_000_000.0
            is String -> {
                val match = durationPattern.matchEntire(value) ?: throw IllegalArgumentException("$path: expected a duration with ms/s/m/t unit")
                val scale = when (match.groupValues[2]) {
                    "ms" -> 1_000_000.0
                    "s" -> 1_000_000_000.0
                    "m" -> 60_000_000_000.0
                    else -> 50_000_000.0
                }
                match.groupValues[1].toDouble() * scale
            }
            else -> throw IllegalArgumentException("$path: expected a duration")
        }
        require(nanos.isFinite() && nanos >= 0.0 && nanos <= MAX_NANOS) { "$path: duration must be finite and between zero and 24 hours" }
        return ceil(nanos).toLong()
    }

    fun ticks(value: Any?, path: String): Long = ceil(durationNanos(value, path) / 50_000_000.0).toLong()
}
