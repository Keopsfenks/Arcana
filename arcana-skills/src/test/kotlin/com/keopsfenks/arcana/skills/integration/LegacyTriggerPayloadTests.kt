package com.keopsfenks.arcana.skills.integration
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.execution.EngineFixture
import io.mockk.every
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.*
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

class LegacyTriggerPayloadTests {
    @Test fun manualTriggerPreservesNestedNullLegacyPayloadAndContainsNativeFailure(@TempDir dir: Path) {
        val f = EngineFixture(dir); every { f.plugin.name } returns "Arcana"
        f.engine.registerTrigger(f.plugin, object : SkillTrigger("raw") { override fun listen(fire: Consumer<SkillContext>) = AutoCloseable { } })
        f.write(options = "triggers: [raw]"); f.load()
        val input = f.context().toBuilder().value("slots", java.util.Arrays.asList("main", null)).build()
        assertTrue(f.engine.trigger("raw", input).toCompletableFuture().join().single().isSuccess); assertEquals(1, f.marks.size)
        f.engine.platform().register(f.plugin, SkillComponentType.MECHANIC, SkillKey("arcana", "native_mark"), object : SkillMechanic(SkillDescriptor.of("native_mark", "test")) {
            override fun compile(arguments: SkillArguments, compilation: SkillCompilation) = CompiledSkillMechanic { f.marks.add("native"); CompletableFuture.completedFuture(SkillResult.success()) }
        })
        Files.writeString(dir.resolve("native.yml"), "schema: 2\nid: native\ntriggers: [raw]\neffects: [{type: native_mark}]\n"); f.load()
        val results = f.engine.trigger("raw", input).toCompletableFuture().join()
        assertEquals(listOf("mark", "mark"), f.marks); assertTrue(results.any { it.status() == SkillResult.Status.UNSUPPORTED_CONTEXT }); f.engine.shutdown()
    }
    @Test fun registeredTriggerPreservesNestedNullLegacyPayload(@TempDir dir: Path) {
        val f = EngineFixture(dir); lateinit var emitted: Consumer<SkillContext>
        f.engine.registerTrigger(f.plugin, object : SkillTrigger("raw") {
            override fun listen(fire: Consumer<SkillContext>): AutoCloseable { emitted = fire; return AutoCloseable { } }
        })
        f.write(options = "triggers: [raw]"); f.load()
        emitted.accept(f.context().toBuilder().value("slots", java.util.Arrays.asList("main", null)).build())
        assertEquals(listOf("mark"), f.marks); f.engine.shutdown()
    }
}
