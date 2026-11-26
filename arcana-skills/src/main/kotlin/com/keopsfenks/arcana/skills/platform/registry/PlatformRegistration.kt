package com.keopsfenks.arcana.skills.platform.registry

import com.keopsfenks.arcana.core.skills.SkillRegistration
import com.keopsfenks.arcana.core.skills.platform.*
import org.bukkit.plugin.Plugin
import java.util.concurrent.atomic.AtomicBoolean

class PlatformRegistration<T : SkillPlatformComponent> @JvmOverloads constructor(
    private val owner: Plugin,
    val key: SkillKey,
    val component: T,
    val descriptor: SkillDescriptor,
    private val lifecycle: com.keopsfenks.arcana.skills.platform.lifecycle.SkillLifecycleGate? = null,
    private val removed: (PlatformRegistration<T>) -> Unit
) : SkillRegistration {
    private val active = AtomicBoolean(true)
    override fun getId() = key.toString()
    override fun getOwner() = owner
    override fun isActive() = active.get()
    override fun close() {
        val retired = lifecycle?.locked { active.compareAndSet(true, false) } ?: active.compareAndSet(true, false)
        if (retired) removed(this)
    }
}

typealias RegisteredComponent<T> = PlatformRegistration<T>
