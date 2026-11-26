package com.keopsfenks.arcana.skills.platform.sources

import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.execution.EngineFixture
import io.mockk.every
import io.mockk.mockk
import org.bukkit.plugin.Plugin
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.*
import java.util.function.Consumer
import java.util.logging.Handler
import java.util.logging.LogRecord
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.triggers.SkillTriggerDispatch

class SourceReloadRaceTests {
    @TempDir lateinit var directory: Path

    @Test fun dynamicCallsCannotRunAReplacedDefinitionFromAnOlderSnapshot() {
        val f = EngineFixture(Files.createDirectories(directory.resolve("arcana")))
        f.register("later") { _, _ -> CompiledSkillEffect { execution ->
            execution.delay(1).thenCompose { execution.cast("alpha:one") }
        } }
        fun owner(name: String) = mockk<Plugin>().also { every { it.name } returns name }
        var generation = 0
        fun source(namespace: String, id: String, definitions: () -> List<SkillDefinition>) = object : SkillDefinitionSource {
            override fun key() = SkillKey(namespace, id)
            override fun load() = definitions()
        }
        f.engine.platform().registerSource(owner("Alpha"), source("alpha", "memory") {
            listOf(SkillDefinition(SkillKey("alpha", "one"), mapOf("effects" to listOf(mapOf("type" to "mark", "name" to "generation$generation"))), "alpha.yml"))
        })
        f.engine.platform().registerSource(owner("Beta"), source("beta", "memory") {
            listOf(SkillDefinition(SkillKey("beta", "two"), mapOf("effects" to listOf(mapOf("type" to "later"))), "beta.yml"))
        })
        f.load()
        val pending = f.cast("beta:two")
        generation++
        assertTrue(f.engine.platform().reloadSource(SkillKey("alpha", "memory")).success())
        assertFalse(pending.isDone)
        f.scheduler.advance(1)
        assertFalse(pending.join().isSuccess)
        assertTrue(f.marks.isEmpty(), "Dynamic call executed a retired definition")
    }


    @Test fun replacedQueuedEventsAreCancelledWithoutReportingAComponentFailure() {
        val f = EngineFixture(directory)
        var listener: Consumer<SkillContext>? = null
        f.engine.registerTrigger(f.plugin, object : SkillTrigger("custom") {
            override fun listen(fire: Consumer<SkillContext>): AutoCloseable {
                listener = fire
                return AutoCloseable {}
            }
        })
        f.write(options = "triggers: [custom]")
        f.load()
        val queued = requireNotNull((listener as SkillTriggerDispatch).prepare(f.context()))
        f.load()
        val records = mutableListOf<LogRecord>()
        val handler = object : Handler() {
            override fun publish(record: LogRecord) { records.add(record) }
            override fun flush() {}
            override fun close() {}
        }
        f.plugin.logger.addHandler(handler)
        try {
            queued.run()
            assertTrue(f.marks.isEmpty())
            assertTrue(records.isEmpty(), records.map { it.message }.toString())
        } finally { f.plugin.logger.removeHandler(handler) }
    }

    @Test fun anUnregisteredSourceCannotPublishAfterItsLoaderReturns() {
        val f = EngineFixture(Files.createDirectories(directory.resolve("arcana")))
        val owner = mockk<Plugin>()
        every { owner.name } returns "Alpha"
        val entered = CountDownLatch(1)
        val resume = CountDownLatch(1)
        val key = SkillKey("alpha", "memory")
        f.engine.platform().registerSource(owner, object : SkillDefinitionSource {
            override fun key() = key
            override fun load(): List<SkillDefinition> {
                entered.countDown()
                check(resume.await(3, TimeUnit.SECONDS))
                return listOf(SkillDefinition(SkillKey("alpha", "one"), mapOf("effects" to listOf(mapOf("type" to "mark"))), "memory"))
            }
        })
        val pool = Executors.newFixedThreadPool(2)
        try {
            val reload = pool.submit<com.keopsfenks.arcana.core.skills.SkillReloadResult> { f.engine.platform().reloadSource(key) }
            assertTrue(entered.await(3, TimeUnit.SECONDS))
            pool.submit { f.engine.closeOwner(owner) }.get(2, TimeUnit.SECONDS)
            resume.countDown()
            assertFalse(reload.get(2, TimeUnit.SECONDS).success())
            assertTrue(f.engine.platform().definitions().isEmpty())
        } finally { resume.countDown(); pool.shutdownNow() }
    }
}
