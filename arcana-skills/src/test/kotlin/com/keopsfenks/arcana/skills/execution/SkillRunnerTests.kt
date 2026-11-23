package com.keopsfenks.arcana.skills.execution

import com.keopsfenks.arcana.core.skills.*
import io.mockk.every
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

class SkillRunnerTests {
    @TempDir lateinit var directory: Path

    @Test fun `chance boundaries and rejected permission never consume a cooldown`() {
        val f = EngineFixture(directory)
        f.write(options = "chance: 0\ncooldown: 1s")
        f.load()
        assertEquals(SkillResult.Status.CHANCE_FAILED, f.cast().join().status())
        f.write(options = "chance: 100\ncooldown: 1s\npermission: test.use")
        f.load()
        every { f.caster.hasPermission("test.use") } returns false
        assertEquals(SkillResult.Status.NO_PERMISSION, f.cast().join().status())
        every { f.caster.hasPermission("test.use") } returns true
        assertTrue(f.cast().join().isSuccess)
        assertEquals(SkillResult.Status.COOLDOWN, f.cast().join().status())
        f.clock.set(1_000_000_000)
        assertTrue(f.cast().join().isSuccess)
    }

    @Test fun `effect order survives a delay and nested calls share depth and step budgets`() {
        val f = EngineFixture(directory)
        f.write(effects = "  - type: mark\n    name: first\n  - type: wait\n  - type: mark\n    name: last")
        f.load()
        val result = f.cast()
        assertEquals(listOf("first"), f.marks)
        assertFalse(result.isDone)
        f.scheduler.advance(1)
        assertTrue(result.join().isSuccess)
        assertEquals(listOf("first", "last"), f.marks)
        f.register("loop") { _, _ -> CompiledSkillEffect { it.cast("loop") } }
        f.write("loop", effects = "  - type: loop")
        f.load()
        assertEquals(SkillResult.Status.ERROR, f.cast("loop").join().status())
        f.register("many") { _, _ -> CompiledSkillEffect { execution ->
            execution.execute(List(1025) { CompiledSkillEffect { CompletableFuture.completedFuture(SkillResult.success()) } })
        } }
        f.write("many", effects = "  - type: many")
        f.load()
        assertEquals(SkillResult.Status.ERROR, f.cast("many").join().status())
    }

    @Test fun `target and location operations run in their owning contexts`() {
        val f = EngineFixture(directory, immediate = false)
        f.register("target") { _, _ -> CompiledSkillEffect { execution ->
            execution.onTarget(SkillTarget.VICTIM) {
                f.scheduler.assertOwner(f.victim)
                SkillResult.success()
            }.thenCompose { execution.atTarget(SkillTarget.VICTIM) { location ->
                f.scheduler.assertOwner(location)
                SkillResult.success()
            } }
        } }
        f.write(effects = "  - type: target")
        f.load()
        val cast = f.cast(target = true)
        assertFalse(cast.isDone)
        f.scheduler.advance()
        assertTrue(cast.join().isSuccess, cast.join().message())
        val missing = f.cast()
        f.scheduler.advance()
        assertEquals(SkillResult.Status.NO_TARGET, missing.join().status())
    }

    @Test fun `trigger index dispatches only enabled matching skills and errors isolate`() {
        val f = EngineFixture(directory)
        f.register("broken") { _, _ -> CompiledSkillEffect { throw IllegalStateException("boom") } }
        f.write("one", "triggers: [attack]", "  - type: broken")
        f.write("two", "triggers: [attack]")
        f.write("disabled", "triggers: [attack]\nenabled: false")
        f.write("manual")
        f.load()
        val results = f.engine.trigger("attack", f.context()).toCompletableFuture().join()
        assertEquals(2, results.size)
        assertTrue(results.any { it.status() == SkillResult.Status.ERROR })
        assertEquals(listOf("mark"), f.marks)
    }
}
