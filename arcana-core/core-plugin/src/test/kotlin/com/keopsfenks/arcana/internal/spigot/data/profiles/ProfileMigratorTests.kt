package com.keopsfenks.arcana.internal.spigot.data.profiles

import com.github.fppt.jedismock.RedisServer
import com.keopsfenks.arcana.core.config.ConfigType
import com.keopsfenks.arcana.core.data.keys.PersistentDataKey
import com.keopsfenks.arcana.core.data.keys.PersistentDataKeyType
import com.keopsfenks.arcana.internal.config.ArcanaConfigSection
import com.keopsfenks.arcana.internal.spigot.data.handlers.configOf
import com.keopsfenks.arcana.internal.spigot.data.handlers.impl.RedisPersistentDataHandler
import com.keopsfenks.arcana.internal.spigot.data.handlers.impl.SQLitePersistentDataHandler
import com.keopsfenks.arcana.internal.spigot.data.handlers.impl.YamlPersistentDataHandler
import com.keopsfenks.arcana.internal.spigot.data.handlers.stubArcanaConfigFactory
import com.keopsfenks.arcana.internal.spigot.data.handlers.unstubArcanaConfigFactory
import com.keopsfenks.arcana.util.namespacedKeyOf
import java.io.File
import java.math.BigDecimal
import java.nio.file.Files
import java.util.UUID
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import redis.clients.jedis.JedisPool

/**
 * The migration runs once, on a server that is locked while it happens, and its source is a file
 * players have hand-edited for years -- so every key type has to land and no stray key may stop it.
 */
class ProfileMigratorTests {
    companion object {
        @JvmStatic
        @BeforeAll
        fun stubArcana() = stubArcanaConfigFactory()

        @JvmStatic
        @AfterAll
        fun unstubArcana() = unstubArcanaConfigFactory()
    }

    private val stringKey =
        PersistentDataKey(namespacedKeyOf("migrate", "string"), PersistentDataKeyType.STRING, "")
    private val boolKey =
        PersistentDataKey(namespacedKeyOf("migrate", "bool"), PersistentDataKeyType.BOOLEAN, false)
    private val intKey =
        PersistentDataKey(namespacedKeyOf("migrate", "int"), PersistentDataKeyType.INT, 0)
    private val doubleKey =
        PersistentDataKey(namespacedKeyOf("migrate", "double"), PersistentDataKeyType.DOUBLE, 0.0)
    private val decimalKey = PersistentDataKey(
        namespacedKeyOf("migrate", "decimal"),
        PersistentDataKeyType.BIG_DECIMAL,
        BigDecimal.ZERO
    )
    private val configKey = PersistentDataKey(
        namespacedKeyOf("migrate", "config"),
        PersistentDataKeyType.CONFIG,
        ArcanaConfigSection(ConfigType.JSON)
    )
    private val listKey = PersistentDataKey(
        namespacedKeyOf("migrate", "list"),
        PersistentDataKeyType.STRING_LIST,
        emptyList<String>()
    )
    private val localKey = PersistentDataKey(
        namespacedKeyOf("migrate", "local"),
        PersistentDataKeyType.STRING,
        "",
        true
    )

    private val alice = UUID.randomUUID()

    private fun tempDatabase(): File =
        Files.createTempDirectory("arcana-migrate-test").resolve("data.db").toFile()

    private fun source() = YamlPersistentDataHandler(
        configOf(
            "player" to mapOf(
                alice.toString() to mapOf(
                    "migrate:string" to "hello",
                    "migrate:bool" to true,
                    "migrate:int" to 42,
                    "migrate:double" to 1.5,
                    "migrate:decimal" to "100.50",
                    "migrate:config" to mapOf("nested" to mapOf("value" to 3)),
                    "migrate:list" to listOf("a", "b", "c"),
                    "migrate:local" to "local value"
                ),
                "not-a-uuid" to mapOf("migrate:int" to 9)
            )
        )
    )

    @Test
    fun `every key type lands in the target and the stray key is skipped`() {
        val file = tempDatabase()
        val target = SQLitePersistentDataHandler(file)

        migrateProfiles(
            source(),
            target,
            setOf(stringKey, boolKey, intKey, doubleKey, decimalKey, configKey, listKey, localKey)
        ) { }

        target.shutdown()

        val reader = SQLitePersistentDataHandler(file)
        try {
            assertEquals("hello", reader.read(alice, stringKey))
            assertEquals(true, reader.read(alice, boolKey))
            assertEquals(42, reader.read(alice, intKey))
            assertEquals(1.5, reader.read(alice, doubleKey))
            assertEquals("100.50", reader.read(alice, decimalKey)?.toPlainString())
            assertEquals(3, reader.read(alice, configKey)?.getInt("nested.value"))
            assertEquals(listOf("a", "b", "c"), reader.read(alice, listKey))
            assertEquals("local value", reader.read(alice, localKey))
            assertEquals(setOf(alice), reader.getSavedUUIDs())
        } finally {
            reader.shutdown()
        }
    }

    @Test
    fun `only the requested keys are carried`() {
        // The local migration passes only the locally-stored keys, because the rest belong to the
        // configured handler and are already there.
        val file = tempDatabase()
        val target = SQLitePersistentDataHandler(file)

        migrateProfiles(source(), target, setOf(localKey)) { }

        target.shutdown()

        val reader = SQLitePersistentDataHandler(file)
        try {
            assertEquals("local value", reader.read(alice, localKey))
            assertEquals(null, reader.read(alice, stringKey))
        } finally {
            reader.shutdown()
        }
    }

    @Test
    fun `every key type survives a round trip through redis`() {
        // Into redis from one handler and back out into another, so a type redis encodes
        // differently from the SQL handlers would come back changed.
        val server = RedisServer.newRedisServer().start()
        val keys = setOf(stringKey, boolKey, intKey, doubleKey, decimalKey, configKey, listKey, localKey)

        try {
            fun redis() = RedisPersistentDataHandler(JedisPool(server.host, server.bindPort), "arcana:", 4)

            val intoRedis = redis()
            migrateProfiles(source(), intoRedis, keys) { }
            intoRedis.shutdown()

            val file = tempDatabase()
            val outOfRedis = redis()
            val target = SQLitePersistentDataHandler(file)
            migrateProfiles(outOfRedis, target, keys) { }
            outOfRedis.shutdown()
            target.shutdown()

            val reader = SQLitePersistentDataHandler(file)
            try {
                assertEquals("hello", reader.read(alice, stringKey))
                assertEquals(true, reader.read(alice, boolKey))
                assertEquals(42, reader.read(alice, intKey))
                assertEquals(1.5, reader.read(alice, doubleKey))
                assertEquals("100.50", reader.read(alice, decimalKey)?.toPlainString())
                assertEquals(3, reader.read(alice, configKey)?.getInt("nested.value"))
                assertEquals(listOf("a", "b", "c"), reader.read(alice, listKey))
                assertEquals("local value", reader.read(alice, localKey))
                assertEquals(setOf(alice), reader.getSavedUUIDs())
            } finally {
                reader.shutdown()
            }
        } finally {
            server.stop()
        }
    }
}
