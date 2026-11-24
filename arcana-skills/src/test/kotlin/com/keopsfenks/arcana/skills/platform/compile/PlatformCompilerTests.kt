package com.keopsfenks.arcana.skills.platform.compile

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.execution.PlatformFixture
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class PlatformCompilerTests {
    @TempDir lateinit var directory: Path
    private fun fixture(): PlatformFixture = PlatformFixture(directory).also { f ->
        f.register("native_mark") { _, _ -> CompiledSkillMechanic { f.legacy.marks.add(it.invocation().data()["mark"]?.text() ?: "original"); f.success() } }
    }
    @Test fun componentNestingAllows64LevelsAndRejectsThe65thRatherThanCountingContainers() {
        val f = fixture()
        f.register("nested") { args, compiler ->
            val left = args.number("left", 1.0).toInt()
            val children = if (left <= 1) emptyList() else compiler.effects(listOf(mapOf("type" to "nested", "left" to left - 1)), "${args.path}.children")
            CompiledSkillMechanic { it.execute(children) }
        }
        f.write(effects = "  - type: nested\n    left: 64")
        val valid = f.engine.reload()
        assertTrue(valid.success(), valid.errors().toString())
        f.write(effects = "  - type: nested\n    left: 65")
        assertFalse(f.engine.reload().success())
    }
    @Test fun targetThenMutatorThenFreshOwnerSnapshotsPrecedeNodeConditionsAndExecution() {
        val f = fixture()
        val order = mutableListOf<String>()
        f.platform.register(f.legacy.plugin, SkillComponentType.TARGETER, SkillKey("arcana", "select_test"), object : SkillTargeter(SkillDescriptor.of("select_test", "test")) {
            override fun compile(arguments: SkillArguments, compilation: SkillCompilation) = CompiledSkillTargeter { order.add("select"); java.util.concurrent.CompletableFuture.completedFuture(SkillSelection(listOf(SkillActor.of(f.legacy.victim)), emptyList())) }
        })
        f.platform.register(f.legacy.plugin, SkillComponentType.MUTATOR, SkillKey("arcana", "actor_test"), object : SkillMutator(SkillDescriptor.of("actor_test", "test")) {
            override fun compile(arguments: SkillArguments, compilation: SkillCompilation) = CompiledSkillMutator { order.add("mutate"); it.derive().actor(it.target().orElseThrow()).build() }
        })
        f.platform.register(f.legacy.plugin, SkillComponentType.PREDICATE, SkillKey("arcana", "snapshot_test"), object : SkillPredicateType(SkillDescriptor.of("snapshot_test", "test")) {
            override fun compile(arguments: SkillArguments, compilation: SkillCompilation) = SkillPredicate { order.add("condition"); it.data().getValue("caster_name").text() == "Victim" }
        })
        f.register("apply_test") { _, _ -> CompiledSkillMechanic { execution -> execution.on(execution.invocation().actor()) { entity -> f.scheduler.assertOwner(entity); order.add("execute"); SkillResult.success() } } }
        f.write(effects = "  - type: apply_test\n    targeter: select_test\n    mutators: [{type: actor_test}]\n    conditions: [{type: snapshot_test}]")
        f.load()
        assertTrue(f.cast().join().isSuccess)
        assertEquals(listOf("select", "mutate", "condition", "execute"), order)
    }
    @Test fun sequentialNodeViewsKeepInvocationVariablesAcrossRepeatsButNotMutatedData() {
        val f = fixture()
        f.register("bump") { _, _ -> CompiledSkillMechanic { execution ->
            execution.services().require(SkillVariables::class.java).add(SkillVariableScope.INVOCATION, SkillKey("arcana", "counter"), 1.0)
            f.legacy.marks.add(execution.invocation().data().getValue("var_counter").number().toString())
            f.success()
        } }
        f.write(options = "bindings:\n  var_counter:\n    key: counter\n    default: 0", effects = "  - type: bump\n    repeat: 3\n  - type: bump")
        f.load(); assertTrue(f.cast().join().isSuccess)
        assertEquals(listOf("1.0", "2.0", "3.0", "4.0"), f.legacy.marks)
    }
    @Test fun eachSelectedTargetGetsAnIsolatedLocalFrameAndSharedRootBudget() {
        val f = fixture()
        f.platform.register(f.legacy.plugin, SkillComponentType.TARGETER, SkillKey("arcana", "two_targets"), object : SkillTargeter(SkillDescriptor.of("two_targets", "test")) {
            override fun compile(arguments: SkillArguments, compilation: SkillCompilation) = CompiledSkillTargeter {
                java.util.concurrent.CompletableFuture.completedFuture(SkillSelection(listOf(SkillActor.of(f.legacy.caster), SkillActor.of(f.legacy.victim)), emptyList()))
            }
        })
        f.register("bump") { _, _ -> CompiledSkillMechanic { execution ->
            execution.services().require(SkillVariables::class.java).add(SkillVariableScope.INVOCATION, SkillKey("arcana", "counter"), 1.0)
            f.legacy.marks.add(execution.invocation().data().getValue("var_counter").number().toString())
            f.success()
        } }
        f.write(options = "bindings:\n  var_counter:\n    key: counter\n    default: 0", effects = "  - type: bump\n    targeter: two_targets\n  - type: bump")
        f.load(); assertTrue(f.cast().join().isSuccess)
        assertEquals(listOf("1.0", "1.0", "1.0"), f.legacy.marks)
    }
    @Test fun nodeMutationsAreIsolatedFromSiblingsAndParentInvocations() {
        val f = fixture()
        f.platform.register(f.legacy.plugin, SkillComponentType.MUTATOR, SkillKey("arcana", "mark_copy"), object : SkillMutator(SkillDescriptor.of("mark_copy", "test")) {
            override fun compile(arguments: SkillArguments, compilation: SkillCompilation) = CompiledSkillMutator { it.derive().data(mapOf("mark" to arguments.string("value"))).build() }
        })
        f.write(effects = "  - type: native_mark\n    mutators:\n      - type: mark_copy\n        value: child\n  - type: native_mark")
        f.load()
        val invocation = f.invocation()
        assertTrue(f.cast(input = invocation).join().isSuccess)
        assertEquals(listOf("child", "original"), f.legacy.marks)
        assertFalse(invocation.data().containsKey("mark"))
    }
    @Test fun defaultFailureStopsAndExplicitSkipContinuesWithoutIgnoringCancellation() {
        val f = fixture()
        f.register("fail") { _, _ -> CompiledSkillMechanic { java.util.concurrent.CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.NO_TARGET, "missing")) } }
        f.write(effects = "  - type: fail\n  - type: native_mark"); f.load()
        assertEquals(SkillResult.Status.NO_TARGET, f.cast().join().status()); assertTrue(f.legacy.marks.isEmpty())
        f.write(effects = "  - type: fail\n    on-failure: skip\n  - type: native_mark"); f.load()
        assertTrue(f.cast().join().isSuccess); assertEquals(listOf("original"), f.legacy.marks)
        f.write(effects = "  - type: native_mark\n    delay: 1t\n    on-failure: skip\n  - type: native_mark"); f.load()
        val pending = f.cast(); f.engine.cancel(f.legacy.playerId)
        assertEquals(SkillResult.Status.CANCELLED, pending.join().status())
    }
    @Test fun repeatAndDelayShareRootAdmissionAndApplyAtBoundedIntervals() {
        val f = fixture()
        f.write(options = "cooldown: 1s", effects = "  - type: native_mark\n    delay: 1t\n    repeat: 3\n    interval: 1t")
        f.load(); val pending = f.cast()
        assertTrue(f.legacy.marks.isEmpty())
        repeat(3) { f.scheduler.advance(1); assertEquals(it + 1, f.legacy.marks.size) }
        assertTrue(pending.join().isSuccess)
        assertEquals(SkillResult.Status.COOLDOWN, f.cast().join().status())
    }
    @Test fun schemaTwoRejectsUnknownNodePartsAndMoreThan1024StaticNodes() {
        val f = fixture()
        for (options in listOf("targeter: {type: absent}", "mutators: [{type: absent}]", "repeat: 101", "chance: 101", "on-failure: ignore", "repeat: param_missing")) {
            f.write(effects = "  - type: native_mark\n    $options")
            val report = f.engine.reload()
            assertFalse(report.success(), options)
            assertTrue(report.errors().any { it.contains("effects") }, report.errors().toString())
        }
        f.write(effects = List(1025) { "  - type: native_mark" }.joinToString("\n"))
        assertFalse(f.engine.reload().success())
        f.write(effects = List(1024) { "  - type: native_mark" }.joinToString("\n"))
        assertTrue(f.engine.reload().success())
    }
}
