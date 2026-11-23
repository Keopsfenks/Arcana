package com.keopsfenks.arcana.skills.execution

import com.keopsfenks.arcana.core.skills.SkillResult
import io.mockk.every
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import com.keopsfenks.arcana.core.skills.SkillEffect
import io.mockk.mockk

class ExecutionLifecycleTests {
    @TempDir lateinit var directory: Path

    @Test fun `registration cannot survive concurrent owner closure or shutdown`() {
        for (shutdown in listOf(false, true)) {
            val f = EngineFixture(directory)
            val entered = CountDownLatch(1)
            val resume = CountDownLatch(1)
            val closing = CountDownLatch(1)
            val component = mockk<SkillEffect>()
            every { component.id } answers { entered.countDown(); check(resume.await(3, TimeUnit.SECONDS)); "late" }
            val pool = Executors.newFixedThreadPool(2)
            try {
                val registered = pool.submit<com.keopsfenks.arcana.core.skills.SkillRegistration> { f.engine.registerEffect(f.plugin, component) }
                assertTrue(entered.await(3, TimeUnit.SECONDS))
                val closed = pool.submit { closing.countDown(); if (shutdown) f.engine.shutdown() else f.engine.closeOwner(f.plugin) }
                assertTrue(closing.await(3, TimeUnit.SECONDS))
                try { closed.get(200, TimeUnit.MILLISECONDS) } catch (_: TimeoutException) { /* Correct admission lock waits for insertion. */ }
                resume.countDown()
                val handle = registered.get(3, TimeUnit.SECONDS)
                closed.get(3, TimeUnit.SECONDS)
                assertFalse(handle.isActive, "Late registration survived shutdown=$shutdown")
            } finally { resume.countDown(); pool.shutdownNow() }
        }
    }

    @Test fun `removal of a statically cast child dependency cancels its waiting ancestors even through cycles`() {
        val f = EngineFixture(directory)
        com.keopsfenks.arcana.skills.BuiltinComponents.install(f.engine, f.plugin)
        val handle = f.register("extension") { _, _ -> com.keopsfenks.arcana.core.skills.CompiledSkillEffect {
            CompletableFuture.completedFuture(SkillResult.success())
        } }
        f.write(options = "cooldown: 3s", effects = "  - type: delay\n    duration: 1s\n  - type: cast\n    skill: child")
        Files.writeString(directory.resolve("child.yml"), "id: child\neffects:\n  - type: extension\n  - type: cast\n    skill: one\n")
        f.load()
        val pending = f.cast()
        assertFalse(pending.isDone)
        handle.close()
        assertTrue(pending.isDone, "Ancestor must be cancelled before its delayed child starts")
        assertEquals(SkillResult.Status.CANCELLED, pending.join().status())
        assertTrue(f.engine.skills().all { it.problem().isNotBlank() })
        assertEquals(SkillResult.Status.DEPENDENCY_UNAVAILABLE, f.cast().join().status())
    }

    @Test fun `failed reload preserves pending work while successful reload cancels old work and keeps cooldown`() {
        val f = EngineFixture(directory)
        f.write(options = "cooldown: 3s", effects = "  - type: wait\n  - type: mark")
        f.load()
        val first = f.cast()
        Files.writeString(directory.resolve("invalid.yml"), "bad: [")
        assertFalse(f.engine.reload().success())
        assertFalse(first.isDone)
        f.scheduler.advance(1)
        assertTrue(first.join().isSuccess)
        Files.delete(directory.resolve("invalid.yml"))
        f.clock.set(3_000_000_000)
        val pending = f.cast()
        assertTrue(f.engine.reload().success())
        assertEquals(SkillResult.Status.CANCELLED, pending.join().status())
        assertEquals(SkillResult.Status.COOLDOWN, f.cast().join().status())
    }

    @Test fun `logout and disable cancel jobs instead of leaving futures pending`() {
        val f = EngineFixture(directory)
        f.write(effects = "  - type: wait\n  - type: mark")
        f.load()
        val logout = f.cast()
        f.engine.cancel(f.playerId)
        assertEquals(SkillResult.Status.CANCELLED, logout.join().status())
        val disable = f.cast()
        f.engine.shutdown()
        assertEquals(SkillResult.Status.CANCELLED, disable.join().status())
        f.scheduler.advance(1)
        assertTrue(f.marks.isEmpty())
        assertEquals(SkillResult.Status.CANCELLED, f.cast().join().status())
    }

    @Test fun `unregister cancels dependent queued callbacks and invalidates active definitions`() {
        val f = EngineFixture(directory)
        var calls = 0
        val handle = f.register("extension") { _, _ -> com.keopsfenks.arcana.core.skills.CompiledSkillEffect {
            calls++
            java.util.concurrent.CompletableFuture.completedFuture(SkillResult.success())
        } }
        f.write(effects = "  - type: wait\n  - type: extension")
        f.load()
        val pending = f.cast()
        handle.close()
        assertEquals(SkillResult.Status.CANCELLED, pending.join().status())
        f.scheduler.advance(1)
        assertEquals(0, calls)
        assertEquals(SkillResult.Status.DEPENDENCY_UNAVAILABLE, f.cast().join().status())
        assertFalse(f.engine.reload().success())
    }

    @Test fun `a cast captured before reload cannot start its old snapshot after the swap`() {
        val f = EngineFixture(directory)
        f.write()
        f.load()
        val entered = CountDownLatch(1)
        val resume = CountDownLatch(1)
        every { f.caster.uniqueId } answers {
            entered.countDown()
            check(resume.await(3, TimeUnit.SECONDS))
            f.playerId
        }
        val pool = Executors.newSingleThreadExecutor()
        try {
            val started = pool.submit<CompletableFuture<SkillResult>> { f.cast() }
            assertTrue(entered.await(3, TimeUnit.SECONDS))
            f.write(effects = "  - type: mark\n    name: new")
            f.load()
            resume.countDown()
            assertEquals(SkillResult.Status.CANCELLED, started.get(3, TimeUnit.SECONDS).join().status())
            assertTrue(f.marks.isEmpty())
        } finally { resume.countDown(); pool.shutdownNow() }
    }

    @Test fun `a callback already running may finish after unregistration but no later step starts`() {
        val f = EngineFixture(directory)
        val pending = CompletableFuture<SkillResult>()
        val handle = f.register("extension") { _, _ -> com.keopsfenks.arcana.core.skills.CompiledSkillEffect { pending } }
        f.write(effects = "  - type: extension\n  - type: mark")
        f.load()
        val cast = f.cast()
        assertFalse(cast.isDone)
        handle.close()
        pending.complete(SkillResult.success())
        assertEquals(SkillResult.Status.CANCELLED, cast.join().status())
        assertTrue(f.marks.isEmpty())
    }

    @Test fun `finished chains release any delayed work they scheduled`() {
        val f = EngineFixture(directory)
        var calls = 0
        f.register("detached") { _, _ -> com.keopsfenks.arcana.core.skills.CompiledSkillEffect { execution ->
            execution.delay(1).thenRun { calls++ }
            CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.ERROR, "stop"))
        } }
        f.write(effects = "  - type: detached")
        f.load()
        assertEquals(SkillResult.Status.ERROR, f.cast().join().status())
        f.scheduler.advance(1)
        assertEquals(0, calls)
    }
}
