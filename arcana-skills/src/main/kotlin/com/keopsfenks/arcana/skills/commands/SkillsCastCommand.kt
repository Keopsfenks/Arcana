package com.keopsfenks.arcana.skills.commands

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.command.impl.Subcommand
import org.bukkit.command.CommandSender

class SkillsCastCommand(plugin: ArcanaPlugin, private val actions: SkillsCommandActions) : Subcommand(plugin, "cast", SkillsCommandActions.PERMISSION, false) {
    override fun onExecute(sender: CommandSender, args: List<String>) = actions.cast(sender, args)
    override fun tabComplete(sender: CommandSender, args: List<String>) = actions.castSuggestions(sender, args)
}
