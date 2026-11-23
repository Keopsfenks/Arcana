package com.keopsfenks.arcana.skills.commands

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.command.impl.Subcommand
import org.bukkit.command.CommandSender

class SkillsReloadCommand(plugin: ArcanaPlugin, private val actions: SkillsCommandActions) : Subcommand(plugin, "reload", SkillsCommandActions.PERMISSION, false) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        if (args.isNotEmpty()) sender.sendMessage("Usage: /arcana skills reload") else actions.reload(sender)
    }
}
