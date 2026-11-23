package com.keopsfenks.arcana.skills.commands

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.skills.SkillContext
import com.keopsfenks.arcana.core.skills.SkillEngine
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class SkillsCommandActions(
    private val plugin: ArcanaPlugin,
    private val engine: () -> SkillEngine,
    private val players: SkillPlayers = BukkitSkillPlayers,
    private val reply: (CommandSender, String) -> Unit = { sender, text -> replyOnOwner(plugin, sender, text) }
) {
    fun list(sender: CommandSender) {
        if (!authorized(sender)) return
        val skills = engine().skills()
        if (skills.isEmpty()) reply(sender, "Arcana Skills: no skills defined.")
        else skills.sortedBy { it.id() }.forEach {
            reply(sender, "${it.id()}${if (it.enabled()) "" else " [disabled]"}${if (it.problem().isBlank()) "" else " [${it.problem()}]"}")
        }
    }

    fun cast(sender: CommandSender, args: List<String>) {
        if (!authorized(sender)) return
        if (args.size !in 1..2) { reply(sender, "Usage: /arcana skills cast <id> [player]"); return }
        val target = if (args.size == 2) players.find(args[1]) else sender as? Player
        if (target == null) { reply(sender, if (args.size == 1) "Console must specify a player." else "Player is not online: ${args[1]}"); return }
        val skillId = args[0]
        engine().cast(skillId, SkillContext.builder(target).build()).whenComplete { result, error ->
            reply(sender, if (error != null) "Skill $skillId: ERROR (${error.message})" else
                "Skill $skillId: ${result.status()}${if (result.message().isBlank()) "" else " (${result.message()})"}")
        }
    }

    fun reload(sender: CommandSender) {
        if (!authorized(sender)) return
        val result = engine().reload()
        if (result.success()) reply(sender, "Reloaded ${result.loaded()} skills (${result.disabled()} disabled).")
        else {
            reply(sender, "Skills reload rejected; active definitions and pending jobs were preserved.")
            result.errors().forEach { plugin.logger.warning("Arcana Skills: $it") }
            result.errors().take(10).forEach { reply(sender, it) }
        }
    }

    fun castSuggestions(sender: CommandSender, args: List<String>): List<String> {
        if (!sender.hasPermission(PERMISSION)) return emptyList()
        return when (args.size) {
            0, 1 -> engine().skills().map { it.id() }.filter { it.startsWith(args.firstOrNull().orEmpty(), ignoreCase = true) }.sorted()
            2 -> players.names().filter { it.startsWith(args[1], ignoreCase = true) }
            else -> emptyList()
        }
    }

    private fun authorized(sender: CommandSender): Boolean {
        if (sender is Player && !sender.hasPermission(PERMISSION)) { reply(sender, "You do not have permission to manage skills."); return false }
        return true
    }

    companion object {
        const val PERMISSION = "arcana.command.skills"

        fun replyOnOwner(plugin: ArcanaPlugin, sender: CommandSender, text: String) {
            try {
                if (sender is Player) plugin.scheduler.on(sender).onRetired {}.run { sender.sendMessage(text) }
                else plugin.scheduler.global().run { sender.sendMessage(text) }
            } catch (_: IllegalStateException) { /* The plugin or recipient may have retired while the cast completed. */ }
        }
    }
}
