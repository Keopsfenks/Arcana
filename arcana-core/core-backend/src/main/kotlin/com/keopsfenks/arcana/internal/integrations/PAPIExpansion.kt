package com.keopsfenks.arcana.internal.integrations

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.integrations.placeholder.PlaceholderManager
import com.keopsfenks.arcana.core.placeholder.context.placeholderContext
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player

@Suppress("DEPRECATION")
class PAPIExpansion(private val plugin: ArcanaPlugin) : PlaceholderExpansion() {
    init {
        register()
    }

    override fun persist(): Boolean {
        return true
    }

    override fun canRegister(): Boolean {
        return true
    }

    override fun getAuthor(): String {
        @Suppress("DEPRECATION")
        return java.lang.String.join(", ", plugin.description.authors)
    }

    override fun getIdentifier(): String {
        @Suppress("DEPRECATION")
        return plugin.description.name.lowercase()
    }

    override fun getVersion(): String {
        @Suppress("DEPRECATION")
        return plugin.description.version
    }

    override fun onPlaceholderRequest(
        player: Player?,
        identifier: String
    ): String? {
        return PlaceholderManager.getResult(
            plugin,
            identifier,
            placeholderContext(
                player = player,
                item = player?.inventory?.itemInMainHand
            )
        )
    }

    override fun getPlaceholders(): List<String> {
        return PlaceholderManager.getRegisteredPlaceholders(plugin)
            .map { "%${this.plugin.name.lowercase()}_${it.displayPattern}%" }
    }
}
