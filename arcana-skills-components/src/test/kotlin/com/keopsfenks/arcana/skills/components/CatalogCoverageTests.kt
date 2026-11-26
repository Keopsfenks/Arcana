package com.keopsfenks.arcana.skills.components
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.diagnostics.SkillSchemaWriter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.*

class CatalogCoverageTests {
    @Test fun catalogRegistersAllSeparateFactoriesAndCopiesCompilableDisabledExamplesWithoutOverwriting(@TempDir dir: Path) {
        val f = NativeComponentFixture(dir); val catalog = StandardSkillCatalog(f.engine, f.root.plugin); catalog.install()
        val descriptors = catalog.descriptors(); assertTrue(descriptors.size >= 110, "${descriptors.size}")
        assertEquals(mapOf("mechanic" to 57, "predicate" to 23, "targeter" to 12, "mutator" to 9, "interceptor" to 3, "contribution" to 2, "aura_component" to 4, "resource" to 3, "stat" to 1, "trigger" to 11), descriptors.groupingBy { it.kind }.eachCount())
        assertTrue(com.keopsfenks.arcana.skills.platform.diagnostics.SkillInspector(f.engine).component("mechanic", SkillKey("arcana", "projectile")).any { it.contains("radius") })
        assertEquals(descriptors.size, descriptors.map { it.kind to it.key }.distinct().size)
        assertTrue(descriptors.filter { it.kind == "mechanic" }.all { it.descriptor.arguments().isNotEmpty() || it.key.value() == "extinguish" })
        val schema = SkillSchemaWriter.write(descriptors); Files.createDirectories(Path.of("build/generated")); Files.writeString(Path.of("build/generated/schema-v2.json"), schema); assertTrue(schema.contains("additionalProperties")); assertTrue(schema.contains("arcana:projectile"))
        assertEquals(requireNotNull(javaClass.getResourceAsStream("/arcana-skills/schema-v2.json")).bufferedReader().readText().trim(), schema.trim())
        StandardSkillExamples.copyTo(dir); val path = dir.resolve(StandardSkillExamples.NAMES.first()); val initial = Files.readString(path)
        Files.writeString(path, "user-owned: true\n"); StandardSkillExamples.copyTo(dir); assertEquals("user-owned: true\n", Files.readString(path))
        Files.writeString(path, initial)
        // Startup copies are deliberately ignored until the operator removes the leading underscore.
        // The registry-heavy lab has its own full-file compiler test with native lookup substitutes.
        assertTrue(Files.exists(dir.resolve("_v2_all-components-test.yml")))
        StandardSkillExamples.NAMES.filterNot { it == "_v2_all-components-test.yml" }.forEach { Files.copy(dir.resolve(it), dir.resolve(it.removePrefix("_"))) }
        f.load()
        assertEquals(6, f.platform.definitions().size)
        assertTrue(f.platform.definitions().filter { it.key().value() != "one" }.all { !it.enabled() })
        f.engine.shutdown(); assertEquals(0, f.platform.managed.size)
    }
}
