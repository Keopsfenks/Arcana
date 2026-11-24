package com.keopsfenks.arcana.skills.registry

import com.keopsfenks.arcana.core.skills.SkillComponent
import org.bukkit.plugin.Plugin
import java.util.concurrent.ConcurrentHashMap

class ComponentRegistry<T : SkillComponent> @JvmOverloads constructor(
    private val lifecycle: com.keopsfenks.arcana.skills.platform.lifecycle.SkillLifecycleGate? = null,
    private val removed: (OwnedRegistration<T>) -> Unit = {}
) {
    constructor(removed: (OwnedRegistration<T>) -> Unit) : this(null, removed)
    private val entries = ConcurrentHashMap<String, OwnedRegistration<T>>()

    fun register(owner: Plugin, component: T): OwnedRegistration<T> {
        val registration = OwnedRegistration(owner, component, lifecycle) { handle ->
            entries.remove(handle.id, handle)
            removed(handle)
        }
        require(entries.putIfAbsent(component.id, registration) == null) { "Component already registered: ${component.id}" }
        return registration
    }

    fun get(id: String): OwnedRegistration<T>? = entries[id]?.takeIf { it.isActive }
    fun snapshot(): List<OwnedRegistration<T>> = entries.values.filter { it.isActive }.sortedBy { it.id }.toList()
    fun closeOwner(owner: Plugin) = snapshot().filter { it.owner === owner }.forEach { it.close() }
    fun close() = snapshot().forEach { it.close() }
}
