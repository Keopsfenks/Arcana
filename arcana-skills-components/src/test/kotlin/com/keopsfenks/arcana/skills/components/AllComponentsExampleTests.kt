package com.keopsfenks.arcana.skills.components

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import java.nio.file.*
import java.util.concurrent.CompletableFuture
import io.mockk.*

class AllComponentsExampleTests {
    private fun fixture(dir: Path) = NativeComponentFixture(dir) { names ->
        mockk<com.keopsfenks.arcana.core.math.ExpressionEnvironment>().also { environment ->
            every { environment.compileOrThrow(any()) } answers {
                val source = firstArg<String>(); val constant = source.toDoubleOrNull(); val slot = names.indexOf(source)
                require(constant != null || slot >= 0) { "Only literal numbers or declared slots are used by this example" }
                object : com.keopsfenks.arcana.core.math.CompiledExpression {
                    override fun evaluate(vararg values: Double) = constant ?: values[slot]
                    override fun evaluate(random: com.keopsfenks.arcana.core.math.RandomSource, vararg values: Double) = evaluate(*values)
                    override fun getVariableCount() = names.size
                    override fun isDeterministic() = true
                }
            }
        }
    }
    private fun text() = requireNotNull(javaClass.getResourceAsStream("/arcana-skills/examples/v2/all-components-test.yml")).bufferedReader().readText()
    private fun document(): Map<String, Any> = Yaml(SafeConstructor(LoaderOptions())).load(text())
    private fun maps(value: Any?): List<Map<*, *>> = when (value) {
        is Map<*, *> -> listOf(value) + value.values.flatMap(::maps)
        is List<*> -> value.flatMap(::maps)
        else -> emptyList()
    }
    @Test fun singleFileLabContainsEveryNativeMechanicAndConditionAndListsActualTriggerRequirements(@TempDir dir: Path) {
        val f = fixture(dir)
        try {
            val catalog = StandardSkillCatalog(f.engine, f.root.plugin); catalog.install()
            val root = document(); val types = maps(root).mapNotNull { it["type"] as? String }.toSet()
            for (kind in listOf("mechanic", "predicate")) {
                val expected = catalog.descriptors().filter { it.kind == kind }.map { it.key.value() }.toSet()
                assertTrue(types.containsAll(expected), "Missing $kind: ${expected - types}")
            }
            assertEquals(false, root["enabled"]); assertEquals(emptyList<Any>(), root["triggers"])
            for (name in listOf("join", "respawn", "death", "block_break", "block_place", "melee_attack", "projectile_hit", "damage_taken", "right_click", "left_click", "sneak", "sprint", "move", "jump", "held_slot", "equip", "consume", "shoot", "kill", "entity_attack", "entity_damaged", "custom", "timer")) assertTrue(text().contains("# $name:"), name)
            assertTrue(text().contains("/arcana skills cast all_components_test"))
        } finally { f.engine.shutdown() }
    }
    @Test fun actualCompilerAcceptsTheWholeLabWithOnlyNativeRegistryLookupsSubstituted(@TempDir dir: Path) {
        val f = fixture(dir)
        try {
            StandardSkillCatalog(f.engine, f.root.plugin).install()
            // These factories resolve host registries while compiling. The remaining real factories,
            // all real predicates, source references, expressions and node limits are exercised.
            for (id in listOf("particle", "particle_line", "particle_ring", "give_item", "remove_item", "enchant", "potion", "remove_potion", "set_block", "break_block", "stun")) {
                val key = SkillKey("arcana", id); val original = f.platform.registry(SkillComponentType.MECHANIC).get(key)!!
                val descriptor = original.component.descriptor(); original.close()
                f.platform.register(f.root.plugin, SkillComponentType.MECHANIC, key, object : SkillMechanic(descriptor) {
                    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
                        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor)
                        return CompiledSkillMechanic { CompletableFuture.completedFuture(SkillResult.success()) }
                    }
                })
            }
            Files.writeString(dir.resolve("all-components-test.yml"), text())
            val report = f.engine.reload(); assertTrue(report.success(), report.errors().joinToString("\n"))
            assertEquals(1, f.platform.definitions().size)
        } finally { f.engine.shutdown() }
    }
    @Test fun failedInGameStepIsReportedAndTheNextStepStillRuns(@TempDir dir: Path) {
        val f = fixture(dir); val messages = mutableListOf<String>()
        try {
            StandardSkillCatalog(f.engine, f.root.plugin).install()
            for (id in listOf("message", "action_bar", "title")) f.platform.registry(SkillComponentType.MECHANIC).get(SkillKey("arcana", id))!!.close()
            f.register("message") { args, _ -> val text = args.string("text"); CompiledSkillMechanic { messages.add(text); f.success() } }
            f.register("action_bar") { _, _ -> CompiledSkillMechanic { CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.NO_TARGET, "Injected rejected native action")) } }
            f.register("title") { _, _ -> CompiledSkillMechanic { f.marks.add("third"); f.success() } }
            val root = document().toMutableMap()
            val runner = maps(root).first { node -> node["type"] == "sequence" && (node["effects"] as? List<*>)?.firstOrNull().let { (it as? Map<*, *>)?.get("key") == "lab_passed" } }
            // Includes the first three actual numbered cases from the delivered file, with their delays/markers.
            root["effects"] = (runner["effects"] as List<*>).take(19)
            root["enabled"] = true
            Files.writeString(dir.resolve("all-components-test.yml"), Yaml().dump(root))
            f.load(); val run = f.cast("all_components_test")
            repeat(100) { f.scheduler.advance(1) }
            assertTrue(run.join().isSuccess)
            assertTrue(messages.any { it.contains("[UYGULANDI] 01/57 message") })
            assertTrue(messages.any { it.contains("[ATLANDI / BAŞARISIZ] 02/57 action_bar") })
            assertFalse(messages.any { it.contains("[UYGULANDI] 02/57 action_bar") })
            assertEquals(listOf("third"), f.marks)
            assertTrue(messages.any { it.contains("[UYGULANDI] 03/57 title") })
        } finally { f.engine.shutdown() }
    }

}
