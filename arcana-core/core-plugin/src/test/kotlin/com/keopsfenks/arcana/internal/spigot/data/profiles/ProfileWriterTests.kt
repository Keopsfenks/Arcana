package com.keopsfenks.arcana.internal.spigot.data.profiles

import com.keopsfenks.arcana.core.config.base.ConfigYml
import com.keopsfenks.arcana.core.data.handlers.PersistentDataHandler
import com.keopsfenks.arcana.core.data.keys.PersistentDataKey
import com.keopsfenks.arcana.core.data.keys.PersistentDataKeyType
import com.keopsfenks.arcana.internal.spigot.ArcanaSpigotPlugin
import com.keopsfenks.arcana.internal.spigot.data.handlers.impl.SQLitePersistentDataHandler
import com.keopsfenks.arcana.internal.spigot.data.handlers.stubArcanaConfigFactory
import com.keopsfenks.arcana.internal.spigot.data.handlers.unstubArcanaConfigFactory
import com.keopsfenks.arcana.internal.spigot.data.profiles.impl.ArcanaPlayerProfile
import com.keopsfenks.arcana.util.namespacedKeyOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 * The writer hands every value on exactly once: a value replaced while the queue drains waits for
 * the next drain rather than being dropped, and a profile loaded again before its values are
 * saved reads the waiting value rather than the stored one.
 */
class ProfileWriterTests {
    companion object {
        @JvmStatic
        @BeforeAll
        fun stubArcana() {
            stubArcanaConfigFactory()
        }

        @JvmStatic
        @AfterAll
        fun unstubArcana() {
            unstubArcanaConfigFactory()
        }
    }

    private val key = PersistentDataKey(namespacedKeyOf("writer", "int"), PersistentDataKeyType.INT, 0)

    private val defaultHandler = mockk<PersistentDataHandler>(relaxed = true)
    private val localHandler = mockk<SQLitePersistentDataHandler>(relaxed = true)
    private val handler = mockk<ProfileHandler>(relaxed = true)
    private val plugin = mockk<ArcanaSpigotPlugin>()
    private val config = mockk<ConfigYml>(relaxed = true)

    init {
        every { handler.defaultHandler } returns defaultHandler
        every { handler.localHandler } returns localHandler
        every { handler.liveMigration } returns null
        every { plugin.configYml } returns config
        every { config.getInt("save-interval") } returns 1
    }

    @Test
    fun `a mid-drain write is kept`() {
        val uuid = UUID.randomUUID()
        val writer = ProfileWriter(plugin, handler)

        every { defaultHandler.write(uuid, key, 1) } answers { writer.write(uuid, key, 2) }

        writer.write(uuid, key, 1)
        writer.flush()

        assertTrue(writer.isPending(uuid, key))
        assertEquals(2, writer.pendingValue(uuid, key))

        writer.flush()

        assertFalse(writer.isPending(uuid, key))
        verify(exactly = 1) { defaultHandler.write(uuid, key, 2) }
    }

    @Test
    fun `close keeps mid-drain write`() {
        val uuid = UUID.randomUUID()
        val writer = ProfileWriter(plugin, handler)

        every { defaultHandler.write(uuid, key, 1) } answers { writer.write(uuid, key, 2) }

        writer.write(uuid, key, 1)
        writer.close()

        assertFalse(writer.isPending(uuid, key))
        verify(exactly = 1) { defaultHandler.write(uuid, key, 2) }
    }

    @Test
    fun `a reload reads pending values`() {
        val uuid = UUID.randomUUID()
        val writer = ProfileWriter(plugin, handler)

        every { handler.profileWriter } returns writer
        every { defaultHandler.read(uuid, key) } returns 1

        writer.write(uuid, key, 5)

        assertEquals(5, ArcanaPlayerProfile(uuid, handler).read(key))
        verify(exactly = 0) { defaultHandler.read(uuid, key) }
    }
}
