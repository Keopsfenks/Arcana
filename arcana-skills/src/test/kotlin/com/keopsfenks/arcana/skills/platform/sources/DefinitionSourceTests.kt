package com.keopsfenks.arcana.skills.platform.sources

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.execution.EngineFixture
import com.keopsfenks.arcana.skills.platform.DefaultSkillPlatform
import io.mockk.every
import io.mockk.mockk
import org.bukkit.plugin.Plugin
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

class DefinitionSourceTests {
    @TempDir lateinit var directory: Path
    private fun fixture() = EngineFixture(Files.createDirectories(directory.resolve("arcana")))
    private fun owner(name: String) = mockk<Plugin>().also { every { it.name } returns name }
    private fun source(key: SkillKey, definitions: () -> List<SkillDefinition>) = object : SkillDefinitionSource {
        override fun key() = key
        override fun load() = definitions()
    }
    private fun legacy(key: SkillKey, wait: Boolean = false) = SkillDefinition(key,
        mapOf("effects" to if (wait) listOf(mapOf("type" to "wait"), mapOf("type" to "mark")) else listOf(mapOf("type" to "mark"))), "memory:$key")

    @Test fun directoryAndMemorySourcesOwnQualifiedIdsWithoutCollisions() {
        val f = fixture()
        val alphaFolder = Files.createDirectories(directory.resolve("alpha"))
        Files.writeString(alphaFolder.resolve("one.yml"), "id: one\neffects:\n  - type: mark\n")
        val alpha = SkillKey("alpha", "files")
        val beta = SkillKey("beta", "memory")
        f.engine.platform().registerSource(owner("Alpha"), DirectoryDefinitionSource(alpha, alphaFolder))
        f.engine.platform().registerSource(owner("Beta"), source(beta) { listOf(legacy(SkillKey("beta", "one"))) })
        assertTrue(f.engine.reload().success())
        assertEquals(setOf(SkillKey("alpha", "one"), SkillKey("beta", "one")), f.engine.platform().definitions().map { it.key() }.toSet())
        assertTrue(f.cast("alpha:one").join().isSuccess)
        assertTrue(f.cast("beta:one").join().isSuccess)
        assertEquals(2, f.marks.size)
        Files.writeString(alphaFolder.resolve("duplicate.yml"), "id: one\neffects:\n  - type: mark\n")
        assertFalse(f.engine.platform().reloadSource(alpha).success())
        assertTrue(f.cast("alpha:one").join().isSuccess)
    }

    @Test fun sourceReloadCancelsChangedWorkButPreservesUnrelatedWorkAndFailedPreflight() {
        val f = fixture()
        var alphaWaits = true
        val alphaSource = SkillKey("alpha", "memory")
        f.engine.platform().registerSource(owner("Alpha"), source(alphaSource) { listOf(legacy(SkillKey("alpha", "one"), alphaWaits)) })
        f.engine.platform().registerSource(owner("Beta"), source(SkillKey("beta", "memory")) { listOf(legacy(SkillKey("beta", "two"), true)) })
        assertTrue(f.engine.reload().success())
        val first = f.cast("alpha:one")
        val second = f.cast("beta:two")
        alphaWaits = false
        assertTrue(f.engine.platform().reloadSource(alphaSource).success())
        assertEquals(SkillResult.Status.CANCELLED, first.join().status())
        assertFalse(second.isDone)
        Files.writeString(f.directory.resolve("broken.yml"), "id: [")
        assertFalse(f.engine.prepareReload().report.success())
        assertFalse(second.isDone)
        f.scheduler.advance(1)
        assertTrue(second.join().isSuccess)
        assertEquals(1, f.marks.size)
    }

    @Test fun cyclicCrossSourceReferencesRetireAncestorsButNotIndependentRevisions() {
        val f = fixture()
        val alphaOwner = owner("Alpha")
        val betaOwner = owner("Beta")
        fun link(id: String) = object : SkillMechanic(SkillDescriptor.of(id, "Declare dependency")) {
            override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
                compilation.referenceSkill(SkillKey.parse(arguments.string("skill"), "arcana"), arguments.path + ".skill")
                return CompiledSkillMechanic { CompletableFuture.completedFuture(SkillResult.success()) }
            }
        }
        f.engine.platform().register(alphaOwner, SkillComponentType.MECHANIC, SkillKey("alpha", "link"), link("link"))
        f.engine.platform().register(betaOwner, SkillComponentType.MECHANIC, SkillKey("beta", "link"), link("link"))
        val alphaSource = SkillKey("alpha", "memory")
        fun definition(key: SkillKey, type: String, reference: String) = SkillDefinition(key,
            mapOf("schema" to 2, "effects" to listOf(mapOf("type" to type, "skill" to reference))), "memory:$key")
        f.engine.platform().registerSource(alphaOwner, source(alphaSource) {
            listOf(definition(SkillKey("alpha", "one"), "alpha:link", "beta:two"))
        })
        f.engine.platform().registerSource(betaOwner, source(SkillKey("beta", "memory")) {
            listOf(definition(SkillKey("beta", "two"), "beta:link", "alpha:one"), legacy(SkillKey("beta", "independent")))
        })
        val result = f.engine.reload()
        assertTrue(result.success(), result.errors().toString())
        val repository = (f.engine.platform() as DefaultSkillPlatform).repository
        val oldDependent = requireNotNull(repository.revision(SkillKey("beta", "two")))
        val independent = requireNotNull(repository.revision(SkillKey("beta", "independent")))
        assertTrue(f.engine.platform().reloadSource(alphaSource).success())
        assertFalse(oldDependent.isActive)
        assertTrue(independent.isActive)
        assertSame(independent, repository.revision(SkillKey("beta", "independent")))
    }

    @Test fun nativeFactoryErrorsCarrySourceAndExactArgumentPath() {
        val f = fixture()
        val key = SkillKey("alpha", "memory")
        f.engine.platform().registerSource(owner("Alpha"), source(key) {
            listOf(SkillDefinition(SkillKey("alpha", "bad"), mapOf("schema" to 2, "effects" to listOf(mapOf("type" to "alpha:missing"))), "bad.yml"))
        })
        val result = f.engine.reload()
        assertFalse(result.success())
        assertTrue(result.errors().any { "bad.yml" in it && "effects[0].type" in it }, result.errors().toString())
    }

    @Test fun explicitNullSchemaOrIdentityCannotFallBackToDefaults() {
        for (field in listOf("schema", "id")) {
            val f = fixture()
            f.engine.platform().registerSource(owner("Alpha"), source(SkillKey("alpha", "memory")) {
                listOf(SkillDefinition(SkillKey("alpha", "bad"), mapOf(field to null, "effects" to listOf(mapOf("type" to "mark"))), "bad.yml"))
            })
            val result = f.engine.reload()
            assertFalse(result.success(), "Explicit null $field was accepted")
            assertTrue(result.errors().any { field in it }, result.errors().toString())
        }
    }
}
