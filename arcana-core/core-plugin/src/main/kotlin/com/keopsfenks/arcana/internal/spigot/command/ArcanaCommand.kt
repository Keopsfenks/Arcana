package com.keopsfenks.arcana.internal.spigot.command

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.command.impl.PluginCommand
import com.keopsfenks.arcana.core.skills.SkillEngine
import com.keopsfenks.arcana.skills.commands.SkillsCommand
import org.bukkit.command.CommandSender

class ArcanaCommand(private val owner: ArcanaPlugin, engine: () -> SkillEngine) : PluginCommand(owner, "arcana", "arcana.command", false) {
    init {
        addSubcommand(SkillsCommand(owner, engine))
        addSubcommand(ArcanaReloadCommand(owner))
    }

    override fun getDescription() = "Arcana framework and skill administration"
    override fun onExecute(sender: CommandSender, args: List<String>) {
        sender.sendMessage("Arcana ${owner.description.version}: /arcana skills | /arcana reload")
    }
}
