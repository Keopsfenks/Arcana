package com.keopsfenks.arcana.skills.platform.registry

import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.lifecycle.SkillLifecycleGate
import org.bukkit.plugin.Plugin
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class PlatformRegistry<T : SkillPlatformComponent>(
    private val type: SkillComponentType<T>,
    private val lifecycle: SkillLifecycleGate,
    private val removed: (PlatformRegistration<T>) -> Unit = {}
) {
    private val entries = ConcurrentHashMap<SkillKey, PlatformRegistration<T>>()

    fun register(owner: Plugin, key: SkillKey, component: T): PlatformRegistration<T> {
        // A component or plugin may execute code while supplying metadata. Do that before admission.
        type.componentClass().cast(component)
        val namespace = namespace(owner)
        require(key.namespace() == namespace) { "Owner namespace is $namespace, cannot register $key" }
        val descriptor = component.descriptor()
        require(key.value() == descriptor.id()) { "Component descriptor ${descriptor.id()} does not match $key" }
        return lifecycle.admit(owner, namespace) {
            val registration = PlatformRegistration(owner, key, component, descriptor, lifecycle) { handle ->
                entries.remove(key, handle)
                removed(handle)
            }
            require(entries.putIfAbsent(key, registration) == null) { "Component already registered: $key" }
            registration
        }
    }

    fun get(key: SkillKey): RegisteredComponent<T>? = entries[key]?.takeIf { it.isActive }
    fun snapshot(): List<PlatformRegistration<T>> = entries.values.filter { it.isActive }.sortedBy { it.key.toString() }

    companion object {
        fun namespace(owner: Plugin): String = owner.name.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9_]"), "_").also {
            require(it.matches(Regex("[a-z0-9_]{1,64}"))) { "Plugin name cannot form a skill namespace" }
        }
    }
}
