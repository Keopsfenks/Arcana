package com.keopsfenks.arcana.internal.spigot.actionbar

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.actionbar.ActionBarProvider
import com.keopsfenks.arcana.core.actionbar.PersistentActionBar
import org.bukkit.NamespacedKey

class ArcanaPersistentActionBar(
    private val key: NamespacedKey,
    private val plugin: ArcanaPlugin,
    private val priority: Int,
    val provider: ActionBarProvider,
    val registrationOrder: Long,
    private val service: PersistentActionBarService
) : PersistentActionBar {
    override fun getKey() = key

    override fun getPlugin() = plugin

    override fun getPriority() = priority

    override fun unregister() = service.unregister(this)
}
