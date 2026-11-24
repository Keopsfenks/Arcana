package com.keopsfenks.arcana.skills.registry

import com.keopsfenks.arcana.core.skills.SkillComponent
import com.keopsfenks.arcana.core.skills.SkillRegistration
import org.bukkit.plugin.Plugin
import java.util.concurrent.atomic.AtomicBoolean

class OwnedRegistration<T : SkillComponent> @JvmOverloads constructor(
    private val owner: Plugin,
    val component: T,
    private val lifecycle: com.keopsfenks.arcana.skills.platform.lifecycle.SkillLifecycleGate? = null,
    private val removed: (OwnedRegistration<T>) -> Unit
) : SkillRegistration {
    private val active = AtomicBoolean(true)
    override fun getId() = component.id
    override fun getOwner() = owner
    override fun isActive() = active.get()
    override fun close() {
        val retired = lifecycle?.locked { active.compareAndSet(true, false) } ?: active.compareAndSet(true, false)
        if (retired) removed(this)
    }
}
