package com.keopsfenks.arcana.skills.integration

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.DefaultSkillEngine
import com.keopsfenks.arcana.skills.compile.CompilerFixture
import com.keopsfenks.arcana.skills.execution.*
import org.bukkit.entity.LivingEntity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.logging.Handler
import java.util.logging.LogRecord

class SkillBoundaryTests {
    @TempDir lateinit var directory: Path

    @Test fun `victim retirement during a delayed effect returns no target for entity and location effects`() {
        for (effect in listOf("heal\n    amount: 2", "sound\n    sound: entity.player.levelup")) {
            val f = EngineFixture(directory)
            var retired = false
            val scheduler = object : SkillScheduler by f.scheduler {
                override fun <T> on(entity: LivingEntity, delayTicks: Long, action: () -> T): ScheduledWork<T> {
                    if (entity === f.victim && retired) return ScheduledWork(CompletableFuture<T>().apply { cancel(false) }) {}
                    return f.scheduler.on(entity, delayTicks, action)
                }
            }
            val engine = DefaultSkillEngine(f.plugin, directory, CompilerFixture().environment, scheduler)
            com.keopsfenks.arcana.skills.BuiltinComponents.install(engine, f.plugin)
            f.write(effects = "  - type: delay\n    duration: 1s\n  - type: $effect\n    target: victim")
            assertTrue(engine.reload().success())
            val cast = engine.cast("one", f.context(target = true)).toCompletableFuture()
            assertFalse(cast.isDone)
            retired = true
            f.scheduler.advance(20)
            assertEquals(SkillResult.Status.NO_TARGET, cast.join().status(), effect)
        }
    }

    @Test fun `a retired victim becomes absent without cancelling caster only effects`() {
        val f = EngineFixture(directory)
        val scheduler = object : SkillScheduler by f.scheduler {
            override fun <T> on(entity: LivingEntity, delayTicks: Long, action: () -> T): ScheduledWork<T> {
                if (entity === f.victim) return ScheduledWork(CompletableFuture<T>().apply { cancel(false) }) {}
                return f.scheduler.on(entity, delayTicks, action)
            }
        }
        val engine = DefaultSkillEngine(f.plugin, directory, CompilerFixture().environment, scheduler)
        var calls = 0
        engine.registerEffect(f.plugin, object : SkillEffect("mark") {
            override fun compile(arguments: SkillArguments, compiler: SkillCompiler) = CompiledSkillEffect {
                calls++
                CompletableFuture.completedFuture(SkillResult.success())
            }
        })
        f.write()
        assertTrue(engine.reload().success())
        val result = engine.cast("one", f.context(target = true)).toCompletableFuture().join()
        assertTrue(result.isSuccess, result.message())
        assertEquals(1, calls)
    }

    @Test fun `automatic runtime errors are identified and rate limited without changing results`() {
        val f = EngineFixture(directory)
        val records = mutableListOf<LogRecord>()
        val logger = f.plugin.logger
        val previous = logger.useParentHandlers
        val handler = object : Handler() {
            override fun publish(record: LogRecord) { records.add(record) }
            override fun flush() {}
            override fun close() {}
        }
        logger.useParentHandlers = false
        logger.addHandler(handler)
        try {
            f.register("broken") { _, _ -> CompiledSkillEffect { throw IllegalStateException("bad effect") } }
            f.write(options = "triggers: [attack]", effects = "  - type: broken")
            f.load()
            repeat(2) {
                val result = f.engine.trigger("attack", f.context()).toCompletableFuture().join().single()
                assertEquals(SkillResult.Status.ERROR, result.status())
            }
            assertEquals(1, records.size)
            assertTrue(records.single().message.contains("one"))
            f.clock.addAndGet(5_000_000_001)
            f.engine.trigger("attack", f.context()).toCompletableFuture().join()
            assertEquals(2, records.size)
        } finally { logger.removeHandler(handler); logger.useParentHandlers = previous }
    }
}
