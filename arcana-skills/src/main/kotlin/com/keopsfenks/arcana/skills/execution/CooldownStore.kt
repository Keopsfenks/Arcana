package com.keopsfenks.arcana.skills.execution

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class CooldownStore(private val clock: () -> Long) {
    internal data class Key(val player: UUID, val group: String)
    private data class Entry(val deadline: Long?, val pending: UUID?)
    private val deadlines = ConcurrentHashMap<Key, Entry>()

    inner class CooldownPermit internal constructor(private val key: Key, private val token: UUID, private val duration: Long) {
        private var phase = 0
        @Synchronized fun commit(): Boolean {
            if (phase != 0) return phase == 1
            var acquired = false
            deadlines.computeIfPresent(key) { _, entry ->
                if (entry.pending == token) {
                    acquired = true
                    if (duration == 0L) null else Entry(clock() + duration, null)
                } else entry
            }
            phase = if (acquired) 1 else 2
            return acquired
        }
        @Synchronized fun release() {
            if (phase == 0) { phase = 2; deadlines.computeIfPresent(key) { _, entry -> if (entry.pending == token) null else entry } }
        }
    }

    fun tryAcquire(player: UUID, group: String, durationNanos: Long): Boolean {
        return reserve(player, group, durationNanos)?.commit() ?: false
    }

    fun reserve(player: UUID, group: String, durationNanos: Long): CooldownPermit? {
        require(durationNanos in 0..86_400_000_000_000L) { "Cooldown out of bounds" }
        val now = clock()
        val token = UUID.randomUUID()
        val key = Key(player, group.removePrefix("arcana:"))
        var admitted = false
        deadlines.compute(key) { _, previous ->
            if (previous == null || previous.pending == null && now - requireNotNull(previous.deadline) >= 0) {
                admitted = true
                Entry(null, token)
            } else previous
        }
        return if (admitted) CooldownPermit(key, token, durationNanos) else null
    }

    fun remaining(player: UUID, group: String): Long = deadlines[Key(player, group.removePrefix("arcana:"))]?.let { entry -> entry.deadline?.let { (it - clock()).coerceAtLeast(0) } ?: 1 } ?: 0
    fun remove(player: UUID) { deadlines.keys.removeIf { it.player == player } }
    fun clear() = deadlines.clear()
}
