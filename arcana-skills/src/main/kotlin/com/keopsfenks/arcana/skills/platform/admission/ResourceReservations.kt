package com.keopsfenks.arcana.skills.platform.admission

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionContext
import com.keopsfenks.arcana.skills.platform.execution.PlatformDependencyException
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture

/** Nonlocal preparation may wait; local holds exist only inside the final owner callback. */
class ResourceReservations(private val context: PlatformExecutionContext, costs: List<CompiledCost>) {
    private data class Cost(val original: CompiledCost, val amount: Double)
    private val costs: List<Cost> = costs.groupBy { it.provider }.mapNotNull { (_, group) ->
        val amount = group.sumOf { it.amount.evaluate(context.invocation()) }
        require(amount.isFinite() && amount >= 0) { "${group.first().path}: combined resource amount must be finite and nonnegative" }
        group.first().provider.component.validateAmount(amount)
        if (amount == 0.0) null else Cost(group.first(), amount)
    }
    private val monitor = Any()
    private val held = mutableListOf<SkillResourceReservation>()
    // 0 preparing, 1 owner callback, 2 irreversible decision, 3 committed, 4 aborted.
    private var phase = 0
    private var cancelled = false
    init {
        context.frame.onClose(::abort)
        context.state.root.result.whenComplete { _, _ -> abort() }
    }
    fun prepare(): CompletableFuture<Unit> {
        var result = CompletableFuture.completedFuture(Unit)
        for (cost in costs.filterNot { it.original.local }) result = result.thenCompose {
            active(cost)
            cost.original.provider.component.reserve(context.invocation().actor(), cost.amount).thenApply { reservation ->
                val accepted = synchronized(monitor) {
                    if (phase != 0 || cancelled) false else { held.add(reservation); true }
                }
                if (!accepted) { release(reservation); throw CancellationException("Resource preparation cancelled") }
                val outcome = reservation.result()
                if (!outcome.isSuccess) throw SkillProviderException(outcome)
                active(cost)
                Unit
            }.toCompletableFuture()
        }
        return result.whenComplete { _, error -> if (error != null) abort() }
    }
    fun enterLocal() {
        synchronized(monitor) {
            if (phase != 0 || cancelled) throw CancellationException("Resource preparation cancelled")
            phase = 1
        }
        for (cost in costs.filter { it.original.local }) {
            active(cost)
            val reservation = cost.original.provider.component.reserveLocal(context.invocation().actor(), cost.amount)
            synchronized(monitor) { held.add(reservation) }
            if (!reservation.result().isSuccess) throw SkillProviderException(reservation.result())
        }
    }
    /** Pure state transition only; no extension callback runs while the admission gate is held. */
    fun decide(): Boolean = synchronized(monitor) {
        if (phase != 1 || cancelled) false else { phase = 2; true }
    }
    fun commit() {
        val reservations = synchronized(monitor) { check(phase == 2); held.toList() }
        try {
            reservations.forEach { it.commit() }
            synchronized(monitor) { phase = 3; held.clear() }
        } catch (error: Throwable) {
            // A provider broke the successful-prepare guarantee. Release holds not yet committed.
            rollback(force = true)
            throw error
        }
    }
    fun abort() {
        val cleanup = synchronized(monitor) {
            cancelled = true
            if (phase != 0) emptyList() else { phase = 4; held.toList().also { held.clear() } }
        }
        cleanup.asReversed().forEach(::release)
    }
    /** Invoked on the same owner callback that prepared local holds. */
    fun rollback(force: Boolean = false) {
        val cleanup = synchronized(monitor) {
            if (phase >= 2 && !force || phase == 3 || phase == 4) emptyList()
            else { phase = 4; cancelled = true; held.toList().also { held.clear() } }
        }
        cleanup.asReversed().forEach(::release)
    }
    private fun active(cost: Cost) {
        context.state.checkActive(); context.frame.checkActive()
        if (!context.platform.componentActive(cost.original.provider)) throw PlatformDependencyException("Resource provider ${cost.original.provider.key} is unavailable")
    }
    private fun release(reservation: SkillResourceReservation) {
        try { reservation.release() } catch (error: Exception) { context.engine.reportProviderFailure("Could not release a resource reservation: ${error.message}") }
    }
}
