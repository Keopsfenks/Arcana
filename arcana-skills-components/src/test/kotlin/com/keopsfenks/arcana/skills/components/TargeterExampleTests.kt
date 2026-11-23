package com.keopsfenks.arcana.skills.components

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.targeters.*
import com.keopsfenks.arcana.skills.execution.EngineFixture
import io.mockk.every
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

class TargeterExampleTests {
    @TempDir lateinit var directory: Path
    @Test fun allTwelveIndependentTargetersHaveCompilingSchemaTwoExamples() {
        val f = EngineFixture(directory)
        every { f.plugin.name } returns "Arcana"
        val platform = f.engine.platform()
        val targeters = listOf(SelfTargeter(), VictimTargeter(), TriggerTargeter(), OriginTargeter(), LocationTargeter(), ForwardTargeter(),
            EntitiesInRadiusTargeter(), PlayersInRadiusTargeter(), BoxTargeter(), ConeTargeter(), LineTargeter(), RayTargeter())
        targeters.forEach { targeter ->
            val id = targeter.descriptor().id()
            platform.register(f.plugin, SkillComponentType.TARGETER, SkillKey("arcana", id), targeter)
            javaClass.getResourceAsStream("/platform-targeters/$id.yml").use { input ->
                assertNotNull(input, "Missing schema 2 example for $id")
                Files.copy(input!!, directory.resolve("$id.yml"))
            }
        }
        platform.register(f.plugin, SkillComponentType.MECHANIC, SkillKey("arcana", "select_fixture"), object : SkillMechanic(SkillDescriptor.of("select_fixture", "test")) {
            override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
                val selector = arguments.section("selector")
                compilation.component(SkillComponentType.TARGETER, SkillKey.parse(selector.string("type"), compilation.namespace())).compile(selector, compilation)
                return CompiledSkillMechanic { CompletableFuture.completedFuture(SkillResult.success()) }
            }
        })
        val report = f.engine.reload()
        assertTrue(report.success(), report.errors().toString())
        assertEquals(12, platform.definitions().size)
        assertEquals(12, targeters.map { it.javaClass }.distinct().size)
        f.engine.shutdown()
    }
}
