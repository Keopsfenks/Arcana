package com.keopsfenks.arcana.skills.platform.sources

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.compile.CompiledSkill
import com.keopsfenks.arcana.skills.compile.SkillSnapshot
import com.keopsfenks.arcana.skills.platform.compile.CompiledPlatformSkill
import com.keopsfenks.arcana.skills.platform.lifecycle.SkillLifecycleGate
import com.keopsfenks.arcana.skills.platform.registry.PlatformRegistry
import org.bukkit.plugin.Plugin
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

/** Source callbacks/factories run outside the gate; only validated immutable publication runs inside. */
class DefinitionRepository(
    private val lifecycle: SkillLifecycleGate,
    private val compileLegacy: (SkillDefinition) -> CompiledSkill,
    private val compileNative: (SkillDefinition) -> CompiledPlatformSkill,
    private val published: (SkillSnapshot) -> (() -> Unit),
    private val invalidated: (SkillRegistration) -> Unit
) {
    private val sources = ConcurrentHashMap<SkillKey, SourceRegistration>()
    private val current = AtomicReference(DefinitionSnapshot.EMPTY)
    private var epoch = 0L
    fun snapshot(): DefinitionSnapshot = current.get()

    fun register(owner: Plugin, source: SkillDefinitionSource, core: Boolean = false): SkillRegistration {
        val key = source.key()
        require(key.namespace() == if (core) "arcana" else PlatformRegistry.namespace(owner)) { "Source namespace does not belong to its owner: $key" }
        return lifecycle.admit(owner, key.namespace()) {
            val handle = SourceRegistration(owner, key, source, ::remove)
            require(sources.putIfAbsent(key, handle) == null) { "Definition source already registered: $key" }
            epoch++
            handle
        }
    }

    fun registrations(): List<SkillRegistration> = sources.values.filter { it.isActive }.toList()
    fun revision(key: SkillKey): DefinitionRevision? = current.get().entries[key]
    fun definitions(): List<SkillDefinitionInfo> = java.util.List.copyOf(current.get().entries.values.map { it.info() })

    fun prepare(keys: Set<SkillKey> = emptySet()): DefinitionCandidate {
        val full = keys.isEmpty()
        val capture = lifecycle.locked { Triple(current.get(), epoch, sources.values.filter { it.isActive }.sortedBy { it.key.toString() }) }
        val (base, generation, registered) = capture
        val selected = if (full) registered else registered.filter { it.key in keys }
        val errors = mutableListOf<String>()
        if (!full) keys.filter { key -> selected.none { it.key == key } }.forEach { errors.add("Unknown definition source: $it") }
        val loaded = LinkedHashMap<SkillKey, DefinitionRevision>()
        for (source in selected) {
            try {
                lifecycle.admit(source.owner) { check(source.isActive) { "Source ${source.key} is unavailable" } }
                val definitions = java.util.List.copyOf(source.source.load())
                for (definition in definitions) {
                    try {
                        require(definition.key().namespace() == source.key.namespace()) { "definition namespace differs from source ${source.key}" }
                        if (definition.configuration().containsKey("id")) {
                            val id = definition.configuration()["id"]
                            require(id is String && SkillKey.parse(id, source.key.namespace()) == definition.key()) { "${definition.origin()}.id: differs from definition key" }
                        }
                        val raw = if (definition.configuration().containsKey("schema")) definition.configuration()["schema"] else 1
                        require(raw is Number && raw.toDouble() in listOf(1.0, 2.0)) { "${definition.origin()}.schema: expected 1 or 2" }
                        val schema = raw.toInt()
                        val legacy = if (schema == 1) compileLegacy(definition) else null
                        val native = if (schema == 2) compileNative(definition) else null
                        val references = legacy?.references?.mapKeys { SkillKey.parse(it.key, source.key.namespace()) } ?: requireNotNull(native).references
                        val dependencies = legacy?.dependencies ?: requireNotNull(native).dependencies
                        val revision = DefinitionRevision(source, definition, schema, legacy, native,
                            Collections.unmodifiableMap(LinkedHashMap(references)), Collections.unmodifiableSet(LinkedHashSet(dependencies)), invalidated)
                        require(loaded.putIfAbsent(definition.key(), revision) == null) { "duplicate skill ID ${definition.key()}" }
                    } catch (error: Exception) { errors.add("${source.key} / ${definition.origin()}: ${error.message ?: error.javaClass.simpleName}") }
                }
            } catch (error: Exception) { errors.add("${source.key}: ${error.message ?: error.javaClass.simpleName}") }
        }
        val selectedKeys = selected.map { it.key }.toSet()
        val merged = LinkedHashMap(base.entries.filterValues { it.source.key !in selectedKeys })
        for ((key, value) in loaded) {
            if (merged.putIfAbsent(key, value) != null) errors.add("${value.definition.origin()}: duplicate skill ID $key across sources")
        }
        loaded.values.forEach { revision -> revision.references.forEach { (key, path) ->
            if (key !in merged) errors.add("$path: unknown skill $key")
        } }
        val changed = base.entries.filterValues { it.source.key in selectedKeys }.keys + loaded.keys
        val affected = DefinitionGraph.affected(changed, base.entries, merged)
        if (errors.isNotEmpty()) return DefinitionCandidate(base, null, generation, registered, affected, loaded.keys.toSet(), full, failure(errors))
        // Ancestors from unchanged sources reuse compiled factories but acquire a new revision lease.
        for (key in affected) if (key !in loaded) merged[key]?.let { merged[key] = it.replacement() }
        for (key in affected) merged[key]?.let { it.materialize(DefinitionGraph.dependencies(it, merged, base.entries)) }
        val snapshot = DefinitionSnapshot(merged.toSortedMap(compareBy { it.toString() }))
        val report = SkillReloadResult(true, loaded.size, loaded.values.count { !it.enabled }, emptyList())
        return DefinitionCandidate(base, snapshot, generation, registered, affected, loaded.keys.toSet(), full, report)
    }

    fun commit(candidate: DefinitionCandidate): SkillReloadResult {
        if (!candidate.report.success()) return candidate.report
        val retired = mutableListOf<DefinitionRevision>()
        var cancelPrevious: () -> Unit = {}
        val result = lifecycle.locked {
            if (candidate.sources.any { !lifecycle.available(it.owner) }) return@locked failure(listOf("Source owner changed during reload"))
            if (current.get() !== candidate.base || epoch != candidate.epoch || candidate.sources.any { !it.isActive || sources[it.key] !== it }) {
                return@locked failure(listOf("Definition sources changed during reload"))
            }
            val snapshot = requireNotNull(candidate.snapshot)
            val missing = candidate.fresh.mapNotNull { snapshot.entries[it] }.filterNot { it.available() }
            if (missing.isNotEmpty()) return@locked failure(missing.map { "${it.definition.origin()}: component was removed during reload" })
            candidate.base.entries.filterKeys { it in candidate.affected }.values.forEach { if (it.retire()) retired.add(it) }
            current.set(snapshot)
            cancelPrevious = published(snapshot.legacy)
            candidate.report
        }
        retired.forEach { it.notifyRetired() }
        if (result.success() && candidate.full) cancelPrevious()
        return result
    }

    private fun remove(source: SourceRegistration) {
        val retired = mutableListOf<DefinitionRevision>()
        lifecycle.locked {
            if (!sources.remove(source.key, source)) return@locked
            epoch++
            val base = current.get()
            val removed = base.entries.filterValues { it.source === source }
            removed.values.forEach { if (it.retire()) retired.add(it) }
            val snapshot = DefinitionSnapshot(base.entries.filterValues { it.source !== source })
            current.set(snapshot)
            published(snapshot.legacy)
        }
        retired.forEach { it.notifyRetired() }
    }

    private fun failure(errors: List<String>) = SkillReloadResult(false, 0, 0, errors)
}
