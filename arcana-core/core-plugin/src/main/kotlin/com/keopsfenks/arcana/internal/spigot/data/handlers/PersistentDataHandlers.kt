package com.keopsfenks.arcana.internal.spigot.data.handlers

import com.keopsfenks.arcana.core.data.handlers.PersistentDataHandler
import com.keopsfenks.arcana.core.registry.KRegistrable
import com.keopsfenks.arcana.core.registry.Registry
import com.keopsfenks.arcana.internal.spigot.ArcanaSpigotPlugin
import com.keopsfenks.arcana.internal.spigot.data.handlers.impl.MariaDBPersistentDataHandler
import com.keopsfenks.arcana.internal.spigot.data.handlers.impl.MongoDBPersistentDataHandler
import com.keopsfenks.arcana.internal.spigot.data.handlers.impl.MySQLPersistentDataHandler
import com.keopsfenks.arcana.internal.spigot.data.handlers.impl.PostgreSQLPersistentDataHandler
import com.keopsfenks.arcana.internal.spigot.data.handlers.impl.RedisPersistentDataHandler
import com.keopsfenks.arcana.internal.spigot.data.handlers.impl.SQLitePersistentDataHandler
import java.io.File

abstract class PersistentDataHandlerFactory(
    override val id: String
): KRegistrable {
    abstract fun create(plugin: ArcanaSpigotPlugin): PersistentDataHandler
}

object PersistentDataHandlers: Registry<PersistentDataHandlerFactory>() {
    init {
        register(object : PersistentDataHandlerFactory("sqlite") {
            override fun create(plugin: ArcanaSpigotPlugin) =
                sqliteHandlerFor(plugin)
        })

        register(object : PersistentDataHandlerFactory("mysql") {
            override fun create(plugin: ArcanaSpigotPlugin) =
                MySQLPersistentDataHandler(plugin.configYml.getSubsection("mysql"))
        })

        register(object : PersistentDataHandlerFactory("mariadb") {
            override fun create(plugin: ArcanaSpigotPlugin) =
                MariaDBPersistentDataHandler(plugin.configYml.getSubsection("mysql"))
        })

        register(object : PersistentDataHandlerFactory("postgresql") {
            override fun create(plugin: ArcanaSpigotPlugin) =
                PostgreSQLPersistentDataHandler(plugin.configYml.getSubsection("postgresql"))
        })

        register(object : PersistentDataHandlerFactory("mongodb") {
            override fun create(plugin: ArcanaSpigotPlugin) =
                MongoDBPersistentDataHandler(plugin.configYml.getSubsection("mongodb"))
        })

        register(object : PersistentDataHandlerFactory("redis") {
            override fun create(plugin: ArcanaSpigotPlugin) =
                RedisPersistentDataHandler.fromConfig(plugin.configYml.getSubsection("redis"))
        })

        // Configs should also accept "mongo"
        register(object : PersistentDataHandlerFactory("mongo") {
            override fun create(plugin: ArcanaSpigotPlugin) =
                MongoDBPersistentDataHandler(plugin.configYml.getSubsection("mongodb"))
        })

        // And "postgres"
        register(object : PersistentDataHandlerFactory("postgres") {
            override fun create(plugin: ArcanaSpigotPlugin) =
                PostgreSQLPersistentDataHandler(plugin.configYml.getSubsection("postgresql"))
        })
    }
}

/**
 * Resolve a configured `data-handler` value to the id of the handler that serves it.
 *
 * yaml is no longer a data handler, but it is the value every long-lived server has in its config,
 * so it resolves to sqlite rather than failing the boot.
 */
fun resolveHandlerId(configured: String): String {
    val id = configured.lowercase()
    return if (id == "yaml") "sqlite" else id
}

/** The SQLite handler for a plugin's own data folder, as both the default and the local handler. */
fun sqliteHandlerFor(plugin: ArcanaSpigotPlugin) = SQLitePersistentDataHandler(
    File(plugin.dataFolder, plugin.configYml.getStringOrNull("sqlite.file") ?: "data.db"),
    plugin.configYml.getStringOrNull("sqlite.prefix") ?: "arcana_"
)

/**
 * The handlers to shut down, with duplicates removed.
 *
 * On a sqlite server the local and default handlers are one object, and shutdown drains that
 * handler's executor -- so shutting it down twice drains an already drained executor.
 */
fun handlersToShutdown(vararg handlers: PersistentDataHandler): List<PersistentDataHandler> =
    handlers.distinct()
