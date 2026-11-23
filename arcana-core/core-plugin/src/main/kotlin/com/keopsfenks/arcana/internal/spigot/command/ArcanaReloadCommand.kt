package com.keopsfenks.arcana.internal.spigot.command

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.command.impl.Subcommand
import com.keopsfenks.arcana.skills.commands.SkillsCommandActions
import org.bukkit.command.CommandSender

class ArcanaReloadCommand(private val owner: ArcanaPlugin) : Subcommand(owner, "reload", "arcana.command.reload", false) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        if (args.isNotEmpty()) { sender.sendMessage("Usage: /arcana reload"); return }
        val accepted = try { owner.tryReload() }
        catch (error: Exception) { owner.logger.warning("Arcana reload failed: ${error.message}"); false }
        SkillsCommandActions.replyOnOwner(owner, sender, if (accepted) "Arcana reloaded." else "Arcana reload rejected; check the console for diagnostics.")
    }
}
