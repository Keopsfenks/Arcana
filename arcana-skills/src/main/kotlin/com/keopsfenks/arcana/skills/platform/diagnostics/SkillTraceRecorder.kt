package com.keopsfenks.arcana.skills.platform.diagnostics
import com.keopsfenks.arcana.core.skills.platform.SkillKey
import java.util.*

/** Opt-in identity summaries only; no invocation, entity, plugin, closure or Event is retained. */
class SkillTraceRecorder(private val clock: () -> Long = System::nanoTime) : AutoCloseable {
    data class Event(val stage: String, val summary: Map<String, String>)
    data class Trace(val id: UUID, val skill: SkillKey, val actor: UUID, val revision: UUID, val events: List<Event>)
    private data class Buffer(val id: UUID, val skill: SkillKey, val actor: UUID, val revision: UUID, val started: Long, val events: ArrayDeque<Event> = ArrayDeque())
    private val enabled = LinkedHashMap<SkillKey, Long>(); private val buffers = LinkedHashMap<UUID, Buffer>()
    private fun prune() { val now = clock(); enabled.entries.removeIf { now - it.value >= TTL }; buffers.entries.removeIf { now - it.value.started >= TTL } }
    @Synchronized fun enable(key: SkillKey) { prune(); if (enabled.size >= 1024 && key !in enabled) enabled.remove(enabled.keys.first()); enabled[key] = clock() }
    @Synchronized fun disable(key: SkillKey) { enabled.remove(key) }
    @Synchronized fun begin(skill: SkillKey, actor: UUID, revision: UUID): UUID? {
        prune(); if (skill !in enabled) return null
        if (buffers.size >= 1024) buffers.remove(buffers.keys.first())
        val id = UUID.randomUUID(); buffers[id] = Buffer(id, skill, actor, revision, clock()); return id
    }
    @Synchronized fun event(id: UUID?, stage: String, summary: Map<String, String> = emptyMap()) {
        if (id == null) return
        prune(); val buffer = buffers[id] ?: return
        if (buffer.events.size == 256) buffer.events.removeFirst()
        val small = LinkedHashMap<String, String>(); summary.entries.take(16).forEach { small[it.key.take(64)] = it.value.take(256) }
        buffer.events.add(Event(stage.take(64), Collections.unmodifiableMap(small)))
    }
    @Synchronized fun traces(): List<Trace> { prune(); return java.util.List.copyOf(buffers.values.map { Trace(it.id, it.skill, it.actor, it.revision, java.util.List.copyOf(it.events)) }) }
    @Synchronized override fun close() { enabled.clear(); buffers.clear() }
    companion object { private const val TTL = 60_000_000_000L }
}
