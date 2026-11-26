package com.keopsfenks.arcana.skills.platform.execution

import com.keopsfenks.arcana.core.skills.platform.SkillKey
import com.keopsfenks.arcana.core.skills.platform.CompiledSkillMechanic
import com.keopsfenks.arcana.skills.platform.DefaultSkillPlatform
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.UUID

class FlowCursorStoreTests {
    @TempDir lateinit var directory: Path
    @Test fun cursorCapsReleasePermitsAndOwnerRemovalDropsEmptyActorSessions() {
        val f = PlatformFixture(directory)
        f.register("native_mark") { _, _ -> CompiledSkillMechanic { f.success() } }
        f.write(); f.load()
        val platform = f.platform as DefaultSkillPlatform
        val revision = platform.repository.snapshot().entries.getValue(SkillKey("arcana", "native"))
        val store = platform.flowCursors
        val actor = UUID.randomUUID()
        repeat(1024) { store.cycle(FlowCursorKey(actor, revision.key, UUID.randomUUID(), revision), 2) {} }
        assertThrows(PlatformBudgetException::class.java) { store.cycle(FlowCursorKey(actor, revision.key, UUID.randomUUID(), revision), 2) {} }
        store.removeActor(actor)
        repeat(64) {
            val id = UUID.randomUUID()
            repeat(1024) { store.cycle(FlowCursorKey(id, revision.key, UUID.randomUUID(), revision), 2) {} }
        }
        assertEquals(65_536, store.size)
        assertThrows(PlatformBudgetException::class.java) { store.cycle(FlowCursorKey(actor, revision.key, UUID.randomUUID(), revision), 2) {} }
        f.engine.closeOwner(f.legacy.plugin)
        assertEquals(0, store.size)
        assertEquals(0, store.actorCount)
    }
}
