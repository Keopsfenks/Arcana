package com.keopsfenks.arcana.skills.components

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.effects.flow.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import io.mockk.every

class FlowMechanicTests {
    @TempDir lateinit var directory: Path
    private fun fixture() = NativeComponentFixture(directory).also { it.install(listOf(SequenceMechanic(), DelayMechanic(), RepeatMechanic(), BranchMechanic(), CastMechanic(), ForEachMechanic(), ParallelMechanic(), WaitUntilMechanic(), WeightedRandomMechanic(), CycleMechanic(), ShuffleMechanic())) }
    @Test fun childCastParameterNamesUseTheSameContractAsTheirDeclarations() {
        val f = fixture()
        val name = "p".repeat(100)
        var received = -1.0
        f.register("parameter_mark") { _, _ -> CompiledSkillMechanic { received = it.invocation().parameters().getValue(name).number(); f.success() } }
        f.write("child", options = "parameters:\n  $name:\n    type: number", effects = "  - type: parameter_mark")
        f.write(effects = "  - type: cast\n    skill: child\n    parameters: {$name: 7}")
        f.load()
        assertTrue(f.cast().join().isSuccess)
        assertEquals(7.0, received)
    }
    @Test fun sequenceBranchRepeatDelayAndParameterizedChildCastPreserveOrder() {
        val f = fixture()
        f.platform.register(f.root.plugin, SkillComponentType.PREDICATE, SkillKey("arcana", "false_test"), object : SkillPredicateType(SkillDescriptor.of("false_test", "test")) {
            override fun compile(arguments: SkillArguments, compilation: SkillCompilation) = SkillPredicate { false }
        })
        var parameter = -1.0
        f.register("parameter_mark") { _, _ -> CompiledSkillMechanic { parameter = it.invocation().parameters().getValue("power").number(); f.marks.add("child"); f.success() } }
        f.write("child", options = "parameters:\n  power:\n    type: number", effects = "  - type: parameter_mark")
        f.write(effects = """
          - type: sequence
            effects:
              - {type: mark, name: first}
              - type: branch
                condition: {type: false_test}
                then: [{type: mark, name: 'yes'}]
                else: [{type: mark, name: 'no'}]
              - type: repeat
                times: 2
                effects: [{type: mark, name: loop}]
              - {type: delay, duration: 1t}
              - type: cast
                skill: child
                parameters: {power: 3}
        """.trimIndent())
        f.load(); val pending = f.cast()
        assertEquals(listOf("first", "no", "loop", "loop"), f.marks)
        f.scheduler.advance(1); assertTrue(pending.join().isSuccess)
        assertEquals(3.0, parameter)
        assertEquals("child", f.marks.last())
    }
    @Test fun delayedBranchesRefreshOwnerSnapshotsBeforeTheirConditionAndBody() {
        val f = fixture()
        f.platform.register(f.root.plugin, SkillComponentType.PREDICATE, SkillKey("arcana", "low"), object : SkillPredicateType(SkillDescriptor.of("low", "test")) {
            override fun compile(arguments: SkillArguments, compilation: SkillCompilation) = SkillPredicate { it.data().getValue("caster_health").number() < 10 }
        })
        var health = -1.0
        f.register("health_mark") { _, _ -> CompiledSkillMechanic { health = it.invocation().data().getValue("caster_health").number(); f.success() } }
        f.write(effects = "  - {type: delay, duration: 1t}\n  - type: branch\n    condition: {type: low}\n    then: [{type: health_mark}]\n    else: [{type: mark, name: stale}]")
        f.load(); val pending = f.cast()
        every { f.root.caster.health } answers { f.scheduler.assertOwner(f.root.caster); 5.0 }
        f.scheduler.advance(1)
        assertTrue(pending.join().isSuccess)
        assertEquals(5.0, health)
        assertTrue(f.marks.isEmpty())
    }
    @Test fun aLaterFailingParallelBranchKeepsItsErrorAndCancelsItsEarlierSibling() {
        val f = fixture()
        f.register("fail") { _, _ -> CompiledSkillMechanic { CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.ERROR, "expected")) } }
        f.write(effects = "  - type: parallel\n    on-failure: skip\n    branches:\n      - effects: [{type: delay, duration: 20t}, {type: mark, name: late}]\n      - effects: [{type: delay, duration: 1t}, {type: fail}]\n  - {type: mark, name: after}")
        f.load(); val pending = f.cast(); f.scheduler.advance(1)
        assertTrue(pending.join().isSuccess)
        assertEquals(listOf("after"), f.marks)
        f.scheduler.advance(20); assertEquals(listOf("after"), f.marks)
    }
    @Test fun weightedChoicesAreDeterministicAndNonpositiveWeightsReject() {
        val f = fixture()
        f.write(effects = "  - type: weighted_random\n    choices:\n      - weight: 1\n        effects: [{type: mark, name: a}]\n      - weight: 3\n        effects: [{type: mark, name: b}]")
        f.load(); assertTrue(f.cast().join().isSuccess); assertEquals("a", f.marks.last())
        f.root.roll = .99; assertTrue(f.cast().join().isSuccess); assertEquals("b", f.marks.last())
        for (weight in listOf(0, -1)) {
            f.write(effects = "  - type: weighted_random\n    choices:\n      - weight: $weight\n        effects: [{type: mark}]")
            assertFalse(f.engine.reload().success())
        }
    }
    @Test fun cycleAndShuffleArePerActorAndRetireTheirStateOnReloadAndLogout() {
        val f = fixture()
        f.write(effects = "  - type: cycle\n    choices:\n      - effects: [{type: mark, name: a}]\n      - effects: [{type: mark, name: b}]")
        f.load(); repeat(3) { assertTrue(f.cast().join().isSuccess) }
        assertEquals(listOf("a", "b", "a"), f.marks)
        assertTrue(f.cast(mob = true).join().isSuccess); assertEquals("a", f.marks.last())
        f.engine.cancel(f.root.playerId)
        assertTrue(f.cast().join().isSuccess); assertEquals("a", f.marks.last())
        f.load(); assertTrue(f.cast().join().isSuccess); assertEquals("a", f.marks.last())
        f.write(effects = "  - type: shuffle\n    choices:\n      - effects: [{type: mark, name: a}]\n      - effects: [{type: mark, name: b}]\n      - effects: [{type: mark, name: c}]")
        f.load(); f.marks.clear(); repeat(3) { assertTrue(f.cast().join().isSuccess) }
        assertEquals(setOf("a", "b", "c"), f.marks.toSet())
    }
    @Test fun foreachCollectsFailuresByDefaultAndSupportsExplicitFailFast() {
        val f = fixture()
        f.platform.register(f.root.plugin, SkillComponentType.TARGETER, SkillKey("arcana", "test_targets"), object : SkillTargeter(SkillDescriptor.of("test_targets", "test")) {
            override fun compile(arguments: SkillArguments, compilation: SkillCompilation) = CompiledSkillTargeter {
                CompletableFuture.completedFuture(SkillSelection(listOf(SkillActor.of(f.root.victim), SkillActor.of(f.root.caster)), emptyList()))
            }
        })
        f.register("fail_victim") { _, _ -> CompiledSkillMechanic { execution ->
            CompletableFuture.completedFuture(if (execution.invocation().target().orElseThrow() == SkillActor.of(f.root.victim)) SkillResult.of(SkillResult.Status.NO_TARGET, "retired") else SkillResult.success())
        } }
        fun write(failFast: Boolean) { f.write(effects = "  - type: for_each\n    select: {type: test_targets}\n    fail-fast: $failFast\n    effects:\n      - type: fail_victim\n      - type: mark") }
        write(false); f.load()
        assertEquals(SkillResult.Status.NO_TARGET, f.cast().join().status()); assertEquals(1, f.marks.size)
        f.marks.clear(); write(true); f.load()
        assertEquals(SkillResult.Status.NO_TARGET, f.cast().join().status()); assertTrue(f.marks.isEmpty())
    }
    @Test fun waitUntilHasFiniteTimeoutAndCancellationReleasesItsPolling() {
        val f = fixture()
        var ready = false
        f.platform.register(f.root.plugin, SkillComponentType.PREDICATE, SkillKey("arcana", "ready"), object : SkillPredicateType(SkillDescriptor.of("ready", "test")) {
            override fun compile(arguments: SkillArguments, compilation: SkillCompilation) = SkillPredicate { ready }
        })
        f.write(effects = "  - type: wait_until\n    condition: {type: ready}\n    timeout: 3t\n    poll: 1t\n  - type: mark")
        f.load(); val pending = f.cast(); repeat(3) { f.scheduler.advance(1) }
        assertEquals(SkillResult.Status.CONDITION_FAILED, pending.join().status()); assertTrue(f.marks.isEmpty())
        val second = f.cast(); ready = true; f.scheduler.advance(1)
        assertTrue(second.join().isSuccess); assertEquals(1, f.marks.size)
        ready = false; val logout = f.cast(); f.engine.cancel(f.root.playerId)
        assertEquals(SkillResult.Status.CANCELLED, logout.join().status())
        ready = true; f.scheduler.advance(5); assertEquals(1, f.marks.size)
    }
}
