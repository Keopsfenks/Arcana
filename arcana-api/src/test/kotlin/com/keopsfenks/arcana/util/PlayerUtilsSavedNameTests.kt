package com.keopsfenks.arcana.util

import com.keopsfenks.arcana.core.Arcana
import com.keopsfenks.arcana.core.data.PlayerProfile
import com.keopsfenks.arcana.core.data.keys.PersistentDataKey
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.bukkit.OfflinePlayer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class PlayerUtilsSavedNameTests {
    private lateinit var arcana: Arcana
    private lateinit var profile: PlayerProfile

    @BeforeEach
    fun setUp() {
        arcana = mockk(relaxed = true)
        profile = mockk(relaxed = true)

        mockkStatic(Arcana::class)
        every { Arcana.get() } returns arcana
        every { arcana.loadPlayerProfile(any()) } returns profile

        // Nothing has ever been saved for the player, so every key reads back its default.
        every { profile.read(any<PersistentDataKey<Any>>()) } answers {
            firstArg<PersistentDataKey<Any>>().defaultValue
        }
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Arcana::class)
    }

    /**
     * A player the server has never seen: their profile was migrated in from another server, so
     * the UUID resolves but Bukkit has no name cached for it.
     */
    private fun unknownPlayer(): OfflinePlayer = mockk(relaxed = true) {
        every { uniqueId } returns UUID.randomUUID()
        every { name } returns null
    }

    @Test
    fun `the keys arcana saves on profiles can be registered up front`() {
        PlayerUtils.registerDataKeys()

        val registered = mutableListOf<PersistentDataKey<*>>()
        verify { arcana.registerPersistentKey(capture(registered)) }

        // The keys are namespaced through Bukkit, which isn't running here, so they're identified
        // by what they hold: a name, a display name, and health.
        val defaults = registered.map { it.defaultValue }.distinct()

        Assertions.assertTrue(defaults.contains("Unknown Player"), "registered: $defaults")
        Assertions.assertTrue(defaults.contains(20.0), "registered: $defaults")
    }

    @Test
    fun `saved display name falls back to a placeholder when the player has no name`() {
        Assertions.assertEquals("Unknown Player", PlayerUtils.getSavedDisplayName(unknownPlayer()))
    }

    @Test
    fun `saved name falls back to a placeholder when the player has no name`() {
        Assertions.assertEquals("Unknown Player", PlayerUtils.getSavedName(unknownPlayer()))
    }
}
