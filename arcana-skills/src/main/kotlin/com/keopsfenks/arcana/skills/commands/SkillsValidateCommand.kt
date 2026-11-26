package com.keopsfenks.arcana.skills.commands
import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.command.impl.Subcommand
import org.bukkit.command.CommandSender
class SkillsValidateCommand(plugin: ArcanaPlugin, private val actions: SkillsCommandActions) : Subcommand(plugin, "validate", SkillsCommandActions.PERMISSION, false) {
    override fun onExecute(sender: CommandSender, args: List<String>) { if (args.isEmpty()) actions.validate(sender) else sender.sendMessage("Usage: /arcana skills validate") }
}
