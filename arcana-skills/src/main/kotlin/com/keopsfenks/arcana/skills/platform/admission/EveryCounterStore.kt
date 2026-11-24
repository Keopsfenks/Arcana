package com.keopsfenks.arcana.skills.platform.admission

import com.keopsfenks.arcana.core.skills.SkillRegistration
import com.keopsfenks.arcana.skills.platform.sources.DefinitionRevision
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class EveryCounterStore {
    private data class Key(val actor: UUID, val revision: DefinitionRevision)
    private val counters = ConcurrentHashMap<Key, Int>()
    fun allow(actor: UUID, revision: DefinitionRevision, every: Int): Boolean {
        require(every in 1..1_000_000)
        if (every == 1) return true
        var allowed = false
        counters.compute(Key(actor, revision)) { _, previous -> ((previous ?: 0) + 1).let { next ->
            if (next >= every) { allowed = true; 0 } else next
        } }
        return allowed
    }
    fun removeActor(actor: UUID) { counters.keys.removeIf { it.actor == actor } }
    fun remove(handle: SkillRegistration) { counters.keys.removeIf { it.revision === handle || handle in it.revision.dependencies } }
    fun clear() = counters.clear()
}
