package com.keopsfenks.arcana.skills.components
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.conditions.VariablePredicate
import io.mockk.*
import org.bukkit.plugin.Plugin
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

class BindingIntegrationTests {
    @Test fun typedEventAndParameterFieldsCompareWithoutExpressionReparsing(@TempDir dir: Path) {
        val f = NativeComponentFixture(dir); f.platform.register(f.root.plugin, SkillComponentType.PREDICATE, SkillKey("arcana", "variable"), VariablePredicate())
        f.write(options = "conditions:\n  - type: variable\n    binding: data.hand\n    equals: OFF_HAND", effects = "  - type: mark"); f.load()
        f.platform.cast(SkillKey("arcana", "one"), f.invocation().derive().data(mapOf("hand" to "OFF_HAND")).build()).toCompletableFuture().join().also { assertTrue(it.isSuccess, it.message()) }
        assertEquals(listOf("mark"), f.marks)
        f.platform.cast(SkillKey("arcana", "one"), f.invocation()).toCompletableFuture().join().also { assertEquals(SkillResult.Status.CONDITION_FAILED, it.status()) }
        f.engine.shutdown()
    }
    @Test fun independentOwnersUseOnlyPublicSourceComponentAndProviderContracts(@TempDir dir: Path) {
        val f = NativeComponentFixture(dir); val api: SkillPlatform = f.platform
        val alpha = mockk<Plugin>(); val beta = mockk<Plugin>(); every { alpha.name } returns "Alpha"; every { beta.name } returns "Beta"
        api.register(beta, SkillComponentType.STAT, SkillKey("beta", "stats"), object : SkillStatProvider {
            override fun descriptor() = SkillDescriptor.of("stats", "test provider")
            override fun snapshot(actor: SkillActor, keys: Set<SkillKey>) = CompletableFuture.completedFuture(keys.associateWith { 7.0 })
        })
        var observed = 0.0
        api.register(beta, SkillComponentType.MECHANIC, SkillKey("beta", "observe"), object : SkillMechanic(SkillDescriptor.of("observe", "test public component")) {
            override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
                val binding = compilation.snapshot(SkillSnapshotRequest(SkillSnapshotRequest.Kind.STAT, SkillSnapshotRequest.Role.ACTOR, "beta:stats", mapOf("key" to SkillValue.of("beta:power"))), arguments.path)
                return CompiledSkillMechanic { context -> observed = context.invocation().data().getValue(binding).number(); f.success() }
            }
        })
        val source = object : SkillDefinitionSource {
            override fun key() = SkillKey("alpha", "source")
            override fun load() = listOf(SkillDefinition(SkillKey("alpha", "one"), mapOf("schema" to 2, "effects" to listOf(mapOf("type" to "beta:observe"))), "alpha-memory:one"))
        }
        api.registerSource(alpha, source); assertTrue(api.reloadSource(source.key()).success())
        api.cast(SkillKey("alpha", "one"), f.invocation()).toCompletableFuture().join().also { assertTrue(it.isSuccess, it.message()) }; assertEquals(7.0, observed)
        f.engine.closeOwner(beta); assertFalse(api.definitions().single { it.key() == SkillKey("alpha", "one") }.available()); f.engine.shutdown()
    }
}
