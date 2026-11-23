package com.keopsfenks.arcana.internal.logging

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.util.StringUtils
import java.util.logging.Level
import java.util.logging.Logger
import org.bukkit.Bukkit

class ArcanaLogger(private val plugin: ArcanaPlugin) : Logger(plugin.name, null as String?) {
    override fun info(msg: String) {
        Bukkit.getConsoleSender().sendMessage("[${plugin.name}] ${StringUtils.format(msg, StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)}")
    }

    init {
        parent = plugin.server.logger
        this.level = Level.ALL
    }
}
