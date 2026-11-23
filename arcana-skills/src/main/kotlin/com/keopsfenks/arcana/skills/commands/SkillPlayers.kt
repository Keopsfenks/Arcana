package com.keopsfenks.arcana.skills.commands

import org.bukkit.Bukkit
import org.bukkit.entity.Player

interface SkillPlayers {
    fun find(name: String): Player?
    fun names(): List<String>
}

object BukkitSkillPlayers : SkillPlayers {
    override fun find(name: String): Player? = Bukkit.getPlayerExact(name)
    override fun names(): List<String> = Bukkit.getOnlinePlayers().map { it.name }.sorted()
}
