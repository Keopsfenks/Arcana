package com.keopsfenks.arcana.skills.platform.execution

import com.keopsfenks.arcana.core.skills.SkillRegistration
import com.keopsfenks.arcana.core.skills.platform.SkillKey
import com.keopsfenks.arcana.skills.platform.sources.DefinitionRevision
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.DoubleSupplier

data class FlowCursorKey(val actor: UUID, val definition: SkillKey, val node: UUID, val revision: DefinitionRevision)

/** Actor-local cursors retire with the exact compiled revision, rather than surviving a reload by ID. */
class FlowCursorStore : AutoCloseable {
    private class Cursor { var index = 0; var order = emptyList<Int>() }
    private class ActorState { var active = true; val cursors = HashMap<FlowCursorKey, Cursor>() }
    private val actors = ConcurrentHashMap<UUID, ActorState>()
    private val total = AtomicInteger()
    private val closed = AtomicBoolean()
    val size: Int get() = total.get()
    val actorCount: Int get() = actors.size
    fun cycle(key: FlowCursorKey, size: Int, valid: () -> Unit): Int = withCursor(key, valid) { cursor ->
        require(size > 0); val next = cursor.index; cursor.index = (cursor.index + 1) % size; next
    }
    fun shuffle(key: FlowCursorKey, size: Int, random: DoubleSupplier, valid: () -> Unit): Int = withCursor(key, valid) { cursor ->
        require(size > 0)
        if (cursor.order.isEmpty()) {
            val order = (0 until size).toMutableList()
            for (index in order.lastIndex downTo 1) {
                val roll = random.asDouble.also { require(it.isFinite() && it >= 0 && it < 1) { "Invalid random source" } }
                val pick = (roll * (index + 1)).toInt(); val swap = order[index]; order[index] = order[pick]; order[pick] = swap
            }
            cursor.order = order
        }
        cursor.order.first().also { cursor.order = cursor.order.drop(1) }
    }
    private fun withCursor(key: FlowCursorKey, valid: () -> Unit, action: (Cursor) -> Int): Int {
        check(!closed.get()) { "Flow cursors are closed" }
        val actor = actors.computeIfAbsent(key.actor) { ActorState() }
        try {
            val outcome: Int? = synchronized(actor) {
                valid()
                if (!actor.active) return@synchronized null
                check(!closed.get()) { "Flow cursors are closed" }
                if (!key.revision.available()) throw PlatformDependencyException("Flow definition retired")
                val cursor = actor.cursors[key] ?: run {
                    if (actor.cursors.size >= 1024) throw PlatformBudgetException("Actor flow cursor limit exceeds 1024")
                    if (total.incrementAndGet() > 65_536) { total.decrementAndGet(); throw PlatformBudgetException("Engine flow cursor limit exceeds 65536") }
                    Cursor().also { actor.cursors[key] = it }
                }
                val value = action(cursor)
                valid()
                value
            }
            return outcome ?: withCursor(key, valid, action)
        } catch (error: Throwable) {
            synchronized(actor) { if (actor.cursors.isEmpty() && actors.remove(key.actor, actor)) actor.active = false }
            throw error
        }
    }
    fun removeActor(id: UUID) { actors.remove(id)?.let { actor -> synchronized(actor) { actor.active = false; total.addAndGet(-actor.cursors.size); actor.cursors.clear() } } }
    fun remove(handle: SkillRegistration) { actors.entries.forEach { (id, actor) -> synchronized(actor) {
        val removed = actor.cursors.keys.filter { it.revision === handle || handle in it.revision.dependencies }
        removed.forEach { actor.cursors.remove(it) }; total.addAndGet(-removed.size)
        if (actor.cursors.isEmpty() && actors.remove(id, actor)) actor.active = false
    } } }
    override fun close() { if (closed.compareAndSet(false, true)) actors.keys.toList().forEach(::removeActor) }
}
