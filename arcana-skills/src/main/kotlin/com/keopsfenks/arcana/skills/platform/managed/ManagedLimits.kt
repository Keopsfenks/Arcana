package com.keopsfenks.arcana.skills.platform.managed

/** Host settings may reduce these budgets, never raise the measured hard caps. */
data class ManagedLimits(val actorInstances: Int = 64, val engineInstances: Int = 4096, val callbacks: Int = 4096,
                         val steps: Int = 65536, val lifetimeTicks: Long = 1_728_000) {
    init { require(actorInstances in 1..64 && engineInstances in 1..4096 && callbacks in 1..4096 && steps in 1..65536 && lifetimeTicks in 1..1_728_000) { "Managed limits must be positive and no larger than hard caps" } }
}
