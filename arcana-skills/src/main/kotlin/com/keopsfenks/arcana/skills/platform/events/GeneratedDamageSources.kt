package com.keopsfenks.arcana.skills.platform.events
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/** Identity-only bounded guard for native entities whose damage arrives on a later tick/region. */
class GeneratedDamageSources(private val clock: () -> Long = System::nanoTime, private val limit: Int = 4096) : AutoCloseable {
    private data class Entry(val at: Long, val ttl: Long)
    private val entries = HashMap<UUID, Entry>(); private var reserved = 0; private var stopped = false
    private fun prune() { val now = clock(); entries.entries.removeIf { now - it.value.at >= it.value.ttl } }
    @Synchronized fun reserve(): Permit {
        prune(); check(!stopped && entries.size + reserved < limit) { "Generated damage source limit reached" }; reserved++; return Permit()
    }
    inner class Permit : AutoCloseable {
        private val open = AtomicBoolean(true)
        fun bind(id: UUID) { synchronized(this@GeneratedDamageSources) { if (open.compareAndSet(true, false)) { reserved--; if (!stopped) entries[id] = Entry(clock(), 60_000_000_000L) } } }
        override fun close() { synchronized(this@GeneratedDamageSources) { if (open.compareAndSet(true, false)) reserved-- } }
    }
    @Synchronized fun contains(id: UUID): Boolean { prune(); return entries.containsKey(id) }
    @Synchronized fun retire(id: UUID) { if (entries.containsKey(id)) entries[id] = Entry(clock(), 1_000_000_000L) }
    @Synchronized override fun close() { stopped = true; entries.clear() }
}
