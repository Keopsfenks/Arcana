package com.keopsfenks.arcana.skills.platform.lifecycle

import com.keopsfenks.arcana.core.skills.SkillRegistration
import org.bukkit.plugin.Plugin
import java.util.Collections
import java.util.IdentityHashMap

/** One admission/publication gate; collect cleanup under it, invoke cleanup after releasing it. */
class SkillLifecycleGate {
    private val monitor = Any()
    private val disabled = Collections.newSetFromMap(IdentityHashMap<Plugin, Boolean>())
    private val namespaces = HashMap<String, Plugin>()
    private var stopped = false

    fun <T> admit(owner: Plugin, namespace: String? = null, action: () -> T): T = synchronized(monitor) {
        check(!stopped) { "Skill engine is stopped" }
        check(owner !in disabled) { "Component owner is disabled" }
        if (namespace != null) {
            val existing = namespaces[namespace]
            require(existing == null || existing === owner) { "Namespace $namespace is already owned by another plugin" }
            namespaces[namespace] = owner
        }
        action()
    }

    fun <T> locked(action: () -> T): T = synchronized(monitor) { action() }

    fun disable(owner: Plugin, collect: () -> List<SkillRegistration>): List<SkillRegistration> = synchronized(monitor) {
        disabled.add(owner)
        namespaces.entries.removeIf { it.value === owner }
        collect()
    }

    fun enable(owner: Plugin) = synchronized(monitor) { if (!stopped) disabled.remove(owner) else false }

    fun stop(collect: () -> List<SkillRegistration>): List<SkillRegistration>? = synchronized(monitor) {
        if (stopped) null else {
            stopped = true
            namespaces.clear()
            disabled.clear()
            collect()
        }
    }
}
