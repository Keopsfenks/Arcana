package com.keopsfenks.arcana.skills.platform.legacy

import com.keopsfenks.arcana.core.skills.SkillResult
import com.keopsfenks.arcana.core.skills.platform.SkillActor
import com.keopsfenks.arcana.core.skills.platform.SkillInvocation
import com.keopsfenks.arcana.skills.execution.EngineFixture
import io.mockk.every
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.UUID

class LegacyInvocationAdapterTests {
    @TempDir lateinit var directory: Path

    @Test fun mobCannotBecomeALegacyPlayerContext() {
        val f = EngineFixture(directory)
        every { f.victim.uniqueId } returns UUID.randomUUID()
        val invocation = SkillInvocation.builder(SkillActor.of(f.victim)).build()
        val error = assertThrows(LegacyActorException::class.java) { LegacyInvocationAdapter.toLegacy(invocation) }
        assertEquals(SkillResult.Status.UNSUPPORTED_ACTOR, error.result.status())
    }

    @Test fun aPlayerInvocationKeepsTheExistingCastPathWorking() {
        val f = EngineFixture(directory)
        f.write()
        f.load()
        val invocation = LegacyInvocationAdapter.fromLegacy(f.context())
        val legacy = LegacyInvocationAdapter.toLegacy(invocation)
        assertSame(f.caster, legacy.caster)
        assertTrue(f.engine.cast("one", legacy).toCompletableFuture().join().isSuccess)
        assertEquals(listOf("mark"), f.marks)
    }
}
