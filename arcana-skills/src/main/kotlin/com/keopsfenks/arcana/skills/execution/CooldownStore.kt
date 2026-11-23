package com.keopsfenks.arcana.skills.execution

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class CooldownStore(private val clock: () -> Long) {
    private data class Key(val player: UUID, val group: String)
    private val deadlines = ConcurrentHashMap<Key, Long>()

    fun tryAcquire(player: UUID, group: String, durationNanos: Long): Boolean {
        require(durationNanos in 0..86_400_000_000_000L) { "Cooldown out of bounds" }
        if (durationNanos == 0L) return true
        val now = clock()
        val admitted = AtomicBoolean(false)
        deadlines.compute(Key(player, group)) { _, previous ->
            if (previous == null || now - previous >= 0) {
                admitted.set(true)
                now + durationNanos
            } else previous
        }
        return admitted.get()
    }

    fun remaining(player: UUID, group: String): Long = deadlines[Key(player, group)]?.let { (it - clock()).coerceAtLeast(0) } ?: 0
    fun remove(player: UUID) { deadlines.keys.removeIf { it.player == player } }
    fun clear() = deadlines.clear()
}
