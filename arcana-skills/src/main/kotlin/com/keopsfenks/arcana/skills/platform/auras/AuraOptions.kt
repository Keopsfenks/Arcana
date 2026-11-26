package com.keopsfenks.arcana.skills.platform.auras
/** Ticks are finite; refresh cannot extend an instance beyond its hard 24-hour lifetime. */
data class AuraOptions(val durationTicks: Long, val intervalTicks: Long = 20, val stackLimit: Int = 1, val shareCaster: Boolean = false, val refresh: Boolean = true, val overwrite: Boolean = false, val charges: Int = 1) {
    init { require(durationTicks in 1..1_728_000); require(intervalTicks in 1..1_728_000); require(stackLimit in 1..64); require(charges in 1..1_000_000) }
}
