package com.keopsfenks.arcana.internal.spigot.data.profiles

import com.keopsfenks.arcana.core.data.handlers.PersistentDataHandler
import com.keopsfenks.arcana.internal.spigot.data.handlers.handlersToShutdown
import com.keopsfenks.arcana.internal.spigot.data.handlers.resolveHandlerId
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProfileHandlerConstructionTests {
    private class FakeHandler(id: String) : PersistentDataHandler(id) {
        override fun getSavedUUIDs(): Set<UUID> = emptySet()
    }

    @Test
    fun `yaml resolves to sqlite`() {
        // Every existing single-node server has yaml in its config, and arcana must not fail to boot
        // on the value it shipped as the default for years.
        assertEquals("sqlite", resolveHandlerId("yaml"))
        assertEquals("sqlite", resolveHandlerId("YAML"))
    }

    @Test
    fun `other handler ids are lowercased and left alone`() {
        assertEquals("mysql", resolveHandlerId("MySQL"))
        assertEquals("mongodb", resolveHandlerId("mongodb"))
        assertEquals("sqlite", resolveHandlerId("sqlite"))
    }

    @Test
    fun `a shared handler is shut down once`() {
        // shutdown drains the handler's executor; calling it twice on one object drains an already
        // drained executor.
        val shared = FakeHandler("sqlite")

        assertEquals(listOf(shared), handlersToShutdown(shared, shared))
    }

    @Test
    fun `distinct handlers are both shut down`() {
        val local = FakeHandler("sqlite")
        val default = FakeHandler("mysql")

        assertEquals(listOf(local, default), handlersToShutdown(local, default))
    }
}
