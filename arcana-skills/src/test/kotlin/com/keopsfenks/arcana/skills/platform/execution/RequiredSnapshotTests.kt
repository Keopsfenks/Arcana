package com.keopsfenks.arcana.skills.platform.execution
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.compile.CompilerFixture
import com.keopsfenks.arcana.skills.execution.ExecutionState
import com.keopsfenks.arcana.skills.platform.compile.PlatformSkillCompiler
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

class RequiredSnapshotTests {
    @TempDir lateinit var directory: Path
    @Test fun unqualifiedStatKeysUseTheResolvedProviderNamespaceInAnotherDefinitionNamespace() {
        val f = PlatformFixture(directory); f.register("native_mark") { _, _ -> CompiledSkillMechanic { f.success() } }
        f.platform.register(f.legacy.plugin, SkillComponentType.PREDICATE, SkillKey("arcana", "stat"), com.keopsfenks.arcana.skills.components.conditions.StatPredicate())
        f.platform.register(f.legacy.plugin, SkillComponentType.STAT, SkillKey("arcana", "test_stats"), object : SkillStatProvider {
            override fun descriptor() = SkillDescriptor.of("test_stats", "Provider namespace probe")
            override fun snapshot(actor: SkillActor, keys: Set<SkillKey>) = CompletableFuture.completedFuture(mapOf(SkillKey("arcana", "power") to 7.0)).also {
                f.scheduler.assertOwner(actor.entity()); assertEquals(setOf(SkillKey("arcana", "power")), keys)
            }
        })
        val configuration = mapOf<String, Any>("schema" to 2, "id" to "probe", "conditions" to listOf(mapOf("type" to "arcana:stat", "provider" to "arcana:test_stats", "key" to "power")), "effects" to listOf(mapOf("type" to "arcana:native_mark")))
        val compiled = PlatformSkillCompiler(f.platform, CompilerFixture().environment, "other").compile(SkillDefinition(SkillKey("other", "probe"), configuration, "probe.yml"))
        val root = ExecutionState(f.legacy.playerId, f.platform.repository.snapshot().legacy); root.dependencies.addAll(compiled.dependencies)
        val state = f.platform.attach(root)
        try {
            val result = RequiredSnapshots.capture(f.invocation(), compiled.snapshots, f.scheduler, state, null, f.platform).join()
            assertEquals(7.0, result.data().getValue(compiled.snapshots.single().alias).number())
        } finally { root.cancel("test complete"); f.engine.shutdown() }
    }
    @Test fun aDefinitionRejectsMoreThan256DistinctSnapshotRequestsDuringPreflight() {
        val f = PlatformFixture(directory)
        f.register("many") { args, compiler ->
            repeat(args.number("count", 256.0).toInt()) { compiler.snapshot(SkillSnapshotRequest.of(SkillSnapshotRequest.Kind.PERMISSION, SkillSnapshotRequest.Role.ACTOR, "arcana.test.$it"), args.path) }
            CompiledSkillMechanic { f.success() }
        }
        f.write(effects = "  - {type: many, count: 256}"); f.load()
        f.write(effects = "  - {type: many, count: 257}"); val report = f.engine.reload(); assertFalse(report.success()); assertTrue(report.errors().joinToString().contains("256"))
    }
}
