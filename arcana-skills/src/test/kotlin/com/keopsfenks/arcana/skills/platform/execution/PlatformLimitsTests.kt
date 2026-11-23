package com.keopsfenks.arcana.skills.platform.execution

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class PlatformLimitsTests {
    @TempDir lateinit var directory: Path
    @Test fun aNativeToLegacyCallCannotResetTheRootsStepBudget() {
        val fixture = PlatformFixture(directory)
        var applied = 0
        fixture.legacy.register("many") { _, _ -> CompiledSkillEffect { execution ->
            execution.execute(List(1024) { CompiledSkillEffect { applied++; fixture.success() } })
        } }
        fixture.legacy.write("old", effects = "  - type: many")
        fixture.register("native_mark") { _, _ -> CompiledSkillMechanic { it.cast(SkillKey("arcana", "old"), emptyMap()) } }
        fixture.write(); fixture.load()
        assertEquals(SkillResult.Status.BUDGET_EXCEEDED, fixture.cast().join().status())
        assertEquals(1022, applied)
    }
    @Test fun nestedNativeListsShareTheRoots1024AppliedSteps() {
        val fixture = PlatformFixture(directory)
        var applied = 0
        fixture.register("native_mark") { _, _ -> CompiledSkillMechanic { execution ->
            execution.execute(List(1024) { CompiledSkillMechanic { applied++; fixture.success() } })
        } }
        fixture.write(); fixture.load()
        assertEquals(SkillResult.Status.BUDGET_EXCEEDED, fixture.cast().join().status())
        assertEquals(1023, applied)
    }
    @Test fun recursiveCastsStopAt16RatherThanReenterAsFreshRoots() {
        val fixture = PlatformFixture(directory)
        var calls = 0
        fixture.register("native_mark") { _, _ -> CompiledSkillMechanic { calls++; it.cast(SkillKey("arcana", "native"), emptyMap()) } }
        fixture.write(); fixture.load()
        assertEquals(SkillResult.Status.BUDGET_EXCEEDED, fixture.cast().join().status())
        assertEquals(16, calls)
    }
}
