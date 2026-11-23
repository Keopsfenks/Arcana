package com.keopsfenks.arcana.internal.spigot.proxies

import com.keopsfenks.arcana.core.command.PluginCommandBase
import org.bukkit.command.CommandMap

interface BukkitCommandsProxy {
    fun getCommandMap(): CommandMap

    fun syncCommands()

    fun unregisterCommand(command: PluginCommandBase)
}
