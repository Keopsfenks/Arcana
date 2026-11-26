package com.keopsfenks.arcana.skills.platform.diagnostics
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.execution.PlatformFixture
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

class SkillDiagnosticsTests {
    @Test fun managementDiagnosticsSharePermissionAndDoNotPublishCandidates(@TempDir directory: Path) {
        val f = PlatformFixture(directory); f.register("native_mark") { _, _ -> CompiledSkillMechanic { f.success() } }; f.write(id = "one", effects = "  - type: native_mark"); f.load()
        val messages = mutableListOf<String>(); val actions = com.keopsfenks.arcana.skills.commands.SkillsCommandActions(f.legacy.plugin, { f.engine }, io.mockk.mockk()) { _, text -> messages.add(text) }
        io.mockk.every { f.legacy.caster.hasPermission(com.keopsfenks.arcana.skills.commands.SkillsCommandActions.PERMISSION) } returns false
        actions.validate(f.legacy.caster); actions.inspect(f.legacy.caster, listOf("one")); actions.trace(f.legacy.caster, listOf("one", "on"))
        assertEquals(3, messages.size); assertTrue(messages.all { it.contains("permission") }); messages.clear()
        io.mockk.every { f.legacy.caster.hasPermission(com.keopsfenks.arcana.skills.commands.SkillsCommandActions.PERMISSION) } returns true
        val before = f.platform.definitions(); actions.validate(f.legacy.caster); actions.inspect(f.legacy.caster, listOf("one")); actions.trace(f.legacy.caster, listOf("one", "on")); f.cast("one").join(); actions.trace(f.legacy.caster, listOf("one", "show"))
        assertEquals(before, f.platform.definitions()); assertTrue(messages.any { it.contains("schema=2") }); assertTrue(messages.any { it.contains("SUCCESS") }); f.engine.shutdown()
    }
    @Test fun tracesAreOptInBoundedImmutableAndExpire() {
        val clock = AtomicLong(); val recorder = SkillTraceRecorder(clock::get); val key = SkillKey("alpha", "test"); val actor = UUID.randomUUID(); val revision = UUID.randomUUID()
        assertNull(recorder.begin(key, actor, revision)); recorder.enable(key)
        val id = recorder.begin(key, actor, revision)!!
        repeat(300) { recorder.event(id, "node", mapOf("index" to "$it")) }
        assertEquals(256, recorder.traces().single().events.size)
        repeat(1100) { recorder.begin(key, actor, revision) }; assertEquals(1024, recorder.traces().size)
        clock.set(60_000_000_000); assertTrue(recorder.traces().isEmpty()); assertNull(recorder.begin(key, actor, revision))
    }
    @Test fun validationAndInspectionAreReadOnlyAndTracesObserveActualCasts(@TempDir directory: Path) {
        val f = PlatformFixture(directory); f.register("native_mark") { _, _ -> CompiledSkillMechanic { f.success() } }
        f.write(id = "one", effects = "  - type: native_mark"); f.load(); val before = f.platform.definitions()
        val inspector = SkillInspector(f.engine); assertTrue(inspector.validate().success()); assertEquals(before, f.platform.definitions())
        assertTrue(inspector.inspect(SkillKey("arcana", "one")).any { it.contains("schema=2") })
        f.platform.traces.enable(SkillKey("arcana", "one")); f.cast("one").join()
        assertTrue(f.platform.traces.traces().single().events.any { it.stage == "complete" })
        f.write(id = "one", effects = "  - type: missing"); val result = inspector.validate(); assertFalse(result.success()); assertTrue(result.errors().single().contains("one.yml")); assertTrue(result.errors().single().contains("effects[0]"))
        assertEquals(before, f.platform.definitions()); f.engine.shutdown()
    }
}
