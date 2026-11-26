package com.keopsfenks.arcana.skills.platform.execution

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.legacy.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class LegacyAdapterTests {
    @TempDir lateinit var directory: Path
    @Test fun legacyAdaptersPreservePlayerClosuresAndNeverInvokeThemForMobs() {
        val f = PlatformFixture(directory)
        var applied = 0
        val adapter = LegacyMechanicAdapter(CompiledSkillEffect { execution ->
            execution.onTarget(SkillTarget.CASTER) { f.scheduler.assertOwner(it); applied++; SkillResult.success() }
        })
        f.register("native_mark") { _, _ -> adapter }
        f.write(); f.load()
        assertTrue(f.cast().join().isSuccess)
        assertEquals(1, applied)
        assertEquals(SkillResult.Status.UNSUPPORTED_ACTOR, f.cast(input = f.invocation(true)).join().status())
        assertEquals(1, applied)
        val predicate = LegacyPredicateAdapter(CompiledSkillCondition { applied++; true })
        assertThrows(LegacyActorException::class.java) { predicate.test(f.invocation(true)) }
        assertEquals(1, applied)
    }
    @Test fun invocationServicesExposeOnlyTypedExecutionBoundQueryAndVariables() {
        val f = PlatformFixture(directory)
        f.register("native_mark") { _, _ -> CompiledSkillMechanic { execution ->
            assertNotNull(execution.services().require(SkillTargetQuery::class.java))
            assertNotNull(execution.services().require(SkillVariables::class.java))
            f.success()
        } }
        f.write(); f.load()
        assertTrue(f.cast().join().isSuccess)
    }
}
