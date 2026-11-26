package com.keopsfenks.arcana.skills.execution
import java.util.concurrent.atomic.AtomicInteger
/** Shared only by explicitly adopted managed descendants; ordinary roots own independent budgets. */
class ExecutionStepBudget(val limit: Int) {
    init { require(limit in 1..65536) }
    private val steps = AtomicInteger()
    val applied get() = steps.get()
    fun step() { if (steps.incrementAndGet() > limit) throw EffectStepLimitException(limit) }
}
