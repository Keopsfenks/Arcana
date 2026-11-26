package com.keopsfenks.arcana.skills.platform.sources

import com.keopsfenks.arcana.core.skills.SkillRegistration
import com.keopsfenks.arcana.core.skills.platform.SkillKey

object DefinitionGraph {
    fun affected(changed: Set<SkillKey>, before: Map<SkillKey, DefinitionRevision>, after: Map<SkillKey, DefinitionRevision>): Set<SkillKey> {
        val reverse = HashMap<SkillKey, MutableSet<SkillKey>>()
        for (revision in before.values + after.values) for (reference in revision.references.keys) {
            reverse.getOrPut(reference) { LinkedHashSet() }.add(revision.key)
        }
        val result = LinkedHashSet<SkillKey>()
        val pending = ArrayDeque(changed)
        while (pending.isNotEmpty()) {
            val key = pending.removeLast()
            if (result.add(key)) pending.addAll(reverse[key].orEmpty())
        }
        return result
    }

    fun dependencies(root: DefinitionRevision, entries: Map<SkillKey, DefinitionRevision>, previous: Map<SkillKey, DefinitionRevision>): Set<SkillRegistration> {
        val result = LinkedHashSet<SkillRegistration>()
        val visited = HashSet<SkillKey>()
        val pending = ArrayDeque<SkillKey>()
        pending.add(root.key)
        while (pending.isNotEmpty()) {
            val key = pending.removeLast()
            if (!visited.add(key)) continue
            val revision = entries[key]
            if (revision == null) {
                // An unchanged definition can remain unavailable after its external owner leaves.
                previous[key]?.let { result.add(it) }
                continue
            }
            result.add(revision)
            result.add(revision.source)
            result.addAll(revision.directDependencies)
            if (revision.references.keys.any { it !in entries }) result.addAll(previous[key]?.dependencies.orEmpty().filterNot { it.isActive })
            pending.addAll(revision.references.keys)
        }
        // Preserve known missing dependency leases for previously unavailable, unselected sources.
        return result
    }
}
