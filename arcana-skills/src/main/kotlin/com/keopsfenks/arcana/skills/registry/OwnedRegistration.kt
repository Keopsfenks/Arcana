package com.keopsfenks.arcana.skills.registry

import com.keopsfenks.arcana.core.skills.SkillComponent
import com.keopsfenks.arcana.core.skills.SkillRegistration
import org.bukkit.plugin.Plugin
import java.util.concurrent.atomic.AtomicBoolean

class OwnedRegistration<T : SkillComponent>(
    private val owner: Plugin,
    val component: T,
    private val removed: (OwnedRegistration<T>) -> Unit
) : SkillRegistration {
    private val active = AtomicBoolean(true)
    override fun getId() = component.id
    override fun getOwner() = owner
    override fun isActive() = active.get()
    override fun close() {
        if (active.compareAndSet(true, false)) removed(this)
    }
}
