package com.keopsfenks.arcana.skills.platform.execution

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class PlatformCompatibilityTests {
    @TempDir lateinit var directory: Path
    @Test fun localsForkAcrossAMixedNativeLegacyNativeCallWithoutChangingTheirParent() {
        val f = PlatformFixture(directory)
        val key = SkillKey("arcana", "counter")
        f.legacy.register("call_child") { _, _ -> CompiledSkillEffect { it.cast("child") } }
        f.legacy.write("old", effects = "  - type: call_child")
        f.register("native_mark") { _, _ -> CompiledSkillMechanic { execution ->
            val variables = execution.services().require(SkillVariables::class.java)
            variables.set(SkillVariableScope.INVOCATION, key, SkillValue.of(2), java.time.Duration.ZERO)
            execution.cast(SkillKey("arcana", "old"), emptyMap()).thenApply {
                assertEquals(2.0, variables.get(SkillVariableScope.INVOCATION, key).orElseThrow().number())
                it
            }
        } }
        var child = -1.0
        f.register("read_child") { _, _ -> CompiledSkillMechanic { execution ->
            child = execution.invocation().data().getValue("var_counter").number()
            execution.services().require(SkillVariables::class.java).add(SkillVariableScope.INVOCATION, key, 1.0)
            f.success()
        } }
        f.write()
        f.write("child", options = "bindings:\n  var_counter:\n    key: counter\n    default: 0", effects = "  - type: read_child")
        f.load()
        assertTrue(f.cast().join().isSuccess)
        assertEquals(2.0, child)
    }
    @Test fun oldPlayerApiCanCastNativeDefinitionsAndNativeCanCallLegacy() {
        val fixture = PlatformFixture(directory)
        fixture.legacy.write("old")
        fixture.register("native_mark") { _, _ -> CompiledSkillMechanic { it.cast(SkillKey("arcana", "old"), emptyMap()) } }
        fixture.write(); fixture.load()
        assertTrue(fixture.engine.cast("native", fixture.legacy.context()).toCompletableFuture().join().isSuccess)
        assertEquals(listOf("mark"), fixture.legacy.marks)
        assertTrue(fixture.engine.skills().any { it.id() == "native" })
    }
    @Test fun aMobCannotReachLegacyPlayerClosures() {
        val fixture = PlatformFixture(directory)
        fixture.legacy.write("old"); fixture.load()
        assertEquals(SkillResult.Status.UNSUPPORTED_ACTOR, fixture.cast("old", fixture.invocation(true)).join().status())
        assertTrue(fixture.legacy.marks.isEmpty())
    }
    @Test fun legacyChildCallsCanReachNativeWithoutNewRootAdmissionOrBudget() {
        val fixture = PlatformFixture(directory)
        fixture.register("native_mark") { _, _ -> CompiledSkillMechanic { fixture.legacy.marks.add("native"); fixture.success() } }
        fixture.legacy.register("call_native") { _, _ -> CompiledSkillEffect { it.cast("native") } }
        fixture.legacy.write("old", effects = "  - type: call_native")
        fixture.write(); fixture.load()
        assertTrue(fixture.legacy.cast("old").join().isSuccess)
        assertEquals(listOf("native"), fixture.legacy.marks)
    }
}
