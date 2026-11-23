package com.keopsfenks.arcana.skills.platform.sources

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.compile.CompiledSkill
import com.keopsfenks.arcana.skills.platform.compile.CompiledPlatformSkill
import org.bukkit.plugin.Plugin
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

class SourceRegistration(
    private val owner: Plugin,
    val key: SkillKey,
    val source: SkillDefinitionSource,
    private val removed: (SourceRegistration) -> Unit
) : SkillRegistration {
    private val active = AtomicBoolean(true)
    override fun getOwner() = owner
    override fun getId() = key.toString()
    override fun isActive() = active.get()
    override fun close() { if (active.compareAndSet(true, false)) removed(this) }
}

/** A published definition lease; only the repository retires it, never a mutable YAML document. */
class DefinitionRevision(
    val source: SourceRegistration,
    val definition: SkillDefinition,
    val schema: Int,
    val legacyBase: CompiledSkill?,
    val platformBase: CompiledPlatformSkill?,
    val references: Map<SkillKey, String>,
    val directDependencies: Set<SkillRegistration>,
    private val invalidated: (SkillRegistration) -> Unit
) : SkillRegistration {
    val key: SkillKey get() = definition.key()
    private val active = AtomicBoolean(true)
    var dependencies: Set<SkillRegistration> = emptySet()
        private set
    var legacy: CompiledSkill? = null
        private set
    var native: CompiledPlatformSkill? = null
        private set
    val enabled get() = legacyBase?.enabled ?: requireNotNull(platformBase).enabled
    override fun getOwner() = source.owner
    override fun getId() = key.toString()
    override fun isActive() = active.get()
    fun available() = isActive && dependencies.all { it.isActive }
    fun info() = SkillDefinitionInfo(key, source.key, definition.origin(), schema, enabled, available())
    internal fun materialize(handles: Set<SkillRegistration>) {
        dependencies = Collections.unmodifiableSet(LinkedHashSet(handles))
        legacy = legacyBase?.copy(id = legacyId(key), dependencies = dependencies,
            cooldownGroup = if (key.namespace() == "arcana") legacyBase.cooldownGroup else "${key.namespace()}:${legacyBase.cooldownGroup}",
            references = Collections.unmodifiableMap(references.mapKeys { legacyId(it.key) }))
        native = platformBase?.copy(dependencies = dependencies)
    }
    internal fun replacement() = DefinitionRevision(source, definition, schema, legacyBase, platformBase, references, directDependencies, invalidated)
    internal fun retire() = active.compareAndSet(true, false)
    internal fun notifyRetired() = invalidated(this)
    override fun close() { if (retire()) notifyRetired() }

    companion object { fun legacyId(key: SkillKey) = if (key.namespace() == "arcana") key.value() else key.toString() }
}
