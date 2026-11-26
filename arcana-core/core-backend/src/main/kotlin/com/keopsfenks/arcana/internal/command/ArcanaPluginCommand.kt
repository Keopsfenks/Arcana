package com.keopsfenks.arcana.internal.command

import com.keopsfenks.arcana.core.Arcana
import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.Prerequisite
import com.keopsfenks.arcana.core.command.CommandBase
import com.keopsfenks.arcana.core.command.PluginCommandBase
import org.bukkit.Bukkit

class ArcanaPluginCommand(
    private val parentDelegate: PluginCommandBase,
    plugin: ArcanaPlugin,
    name: String,
    permission: String,
    playersOnly: Boolean
) : HandledCommand(parentDelegate, plugin, name, permission, playersOnly),
    PluginCommandBase {
    override fun register() = mutateOnOwner {
        val command = Bukkit.getPluginCommand(name)

        val knownDescription = command?.description ?: description ?: ""
        val knownAliases = command?.aliases ?: aliases

        if (command == null) {
            unregisterWithoutSync()
            commandMap.register(plugin.name.lowercase(), DelegatedBukkitCommand(this))
        } else {
            command.setExecutor(this)
            command.tabCompleter = this
            command.description = knownDescription
            command.aliases = knownAliases
        }

        Arcana.get().syncCommands()
    }

    override fun unregister() = mutateOnOwner {
        unregisterWithoutSync()
        Arcana.get().syncCommands()
    }

    /** Cleanup, insertion and sync must share one owner callback, including during startup. */
    private inline fun mutateOnOwner(crossinline action: () -> Unit) {
        if (Prerequisite.HAS_FOLIA.isMet && !Bukkit.isGlobalTickThread()) {
            plugin.scheduler.global().run { action() }
        } else {
            action()
        }
    }

    private fun unregisterWithoutSync() {
        commandMap.getCommand(name)?.unregister(commandMap)
        Arcana.get().unregisterCommand(this)
    }

    override fun getAliases(): List<String> = parentDelegate.aliases
    override fun getDescription(): String? = parentDelegate.description
}

class ArcanaSubcommand(
    parentDelegate: CommandBase,
    plugin: ArcanaPlugin,
    name: String,
    permission: String,
    playersOnly: Boolean
) : HandledCommand(parentDelegate, plugin, name, permission, playersOnly)
