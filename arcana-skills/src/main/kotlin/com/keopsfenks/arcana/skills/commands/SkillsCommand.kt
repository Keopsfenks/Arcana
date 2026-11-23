package com.keopsfenks.arcana.skills.commands

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.command.impl.Subcommand
import com.keopsfenks.arcana.core.skills.SkillEngine
import org.bukkit.command.CommandSender

class SkillsCommand(plugin: ArcanaPlugin, engine: () -> SkillEngine) : Subcommand(plugin, "skills", SkillsCommandActions.PERMISSION, false) {
    init {
        val actions = SkillsCommandActions(plugin, engine)
        addSubcommand(SkillsListCommand(plugin, actions))
        addSubcommand(SkillsCastCommand(plugin, actions))
        addSubcommand(SkillsReloadCommand(plugin, actions))
    }

    override fun onExecute(sender: CommandSender, args: List<String>) {
        sender.sendMessage("Usage: /arcana skills list | cast <id> [player] | reload")
    }
}
