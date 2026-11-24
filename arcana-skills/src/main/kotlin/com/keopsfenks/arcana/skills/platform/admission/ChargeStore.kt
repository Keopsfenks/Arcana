package com.keopsfenks.arcana.skills.platform.admission

import com.keopsfenks.arcana.core.skills.platform.SkillKey
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class ChargeOptions(val max: Int, val refillNanos: Long) {
    init { require(max in 1..1_000_000); require(refillNanos in 1..86_400_000_000_000L) }
}

/** Pending holds cannot refill themselves; only committed use starts the refill clock. */
class ChargeStore(private val clock: () -> Long) {
    internal data class Key(val actor: UUID, val group: SkillKey)
    internal class State(var options: ChargeOptions, var last: Long) {
        var active = true
        var spent = 0
        val pending = HashSet<UUID>()
    }
    private val states = ConcurrentHashMap<Key, State>()
    inner class ChargePermit internal constructor(private val state: State, private val token: UUID) {
        private var phase = 0
        fun commit(): Boolean = synchronized(state) {
            if (phase != 0) return@synchronized phase == 1
            if (!state.active || !state.pending.remove(token)) { phase = 2; return@synchronized false }
            refresh(state, state.options)
            if (state.spent == 0) state.last = clock()
            state.spent++; phase = 1
            true
        }
        fun release() = synchronized(state) { if (phase == 0) { state.pending.remove(token); phase = 2 }; Unit }
    }
    fun reserve(actor: UUID, group: SkillKey, options: ChargeOptions): ChargePermit? {
        val state = states.computeIfAbsent(Key(actor, group)) { State(options, clock()) }
        return synchronized(state) {
            if (!state.active) return@synchronized null
            refresh(state, options)
            if (state.options.max - state.spent - state.pending.size <= 0) null
            else UUID.randomUUID().let { token -> state.pending.add(token); ChargePermit(state, token) }
        }
    }
    fun available(actor: UUID, group: SkillKey, options: ChargeOptions): Int {
        val state = states.computeIfAbsent(Key(actor, group)) { State(options, clock()) }
        return synchronized(state) { refresh(state, options); (state.options.max - state.spent - state.pending.size).coerceAtLeast(0) }
    }
    private fun refresh(state: State, options: ChargeOptions) {
        state.options = options; state.spent = state.spent.coerceAtMost(options.max)
        val now = clock()
        if (state.spent > 0) {
            val elapsed = now - state.last
            if (elapsed >= options.refillNanos) {
                val restored = minOf(state.spent.toLong(), elapsed / options.refillNanos).toInt()
                state.spent -= restored
                state.last += restored * options.refillNanos
            }
        }
        if (state.spent == 0) state.last = now
    }
    fun remove(actor: UUID) { states.entries.filter { it.key.actor == actor }.forEach { (key, state) -> if (states.remove(key, state)) synchronized(state) { state.active = false; state.pending.clear() } } }
    fun clear() { states.keys.map { it.actor }.distinct().forEach(::remove) }
}
