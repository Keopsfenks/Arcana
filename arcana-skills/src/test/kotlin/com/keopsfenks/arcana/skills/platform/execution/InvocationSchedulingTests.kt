package com.keopsfenks.arcana.skills.platform.execution

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import io.mockk.every
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class InvocationSchedulingTests {
    @TempDir lateinit var directory: Path
    @Test fun selectiveNativeSourceReloadCancelsOnlyItsOwnRevisionAndRejectsLateDynamicChildren() {
        val f = PlatformFixture(directory)
        var marks = 0
        f.register("native_mark") { _, _ -> CompiledSkillMechanic { execution -> execution.delay(1).thenApply { marks++; it } } }
        f.register("dynamic") { _, _ -> CompiledSkillMechanic { execution -> execution.delay(1).thenCompose { execution.cast(SkillKey("alpha", "one"), emptyMap()) } } }
        fun source(name: String, type: String) {
            val owner = io.mockk.mockk<org.bukkit.plugin.Plugin>()
            every { owner.name } returns name.replaceFirstChar { it.uppercase() }
            f.platform.registerSource(owner, object : SkillDefinitionSource {
                override fun key() = SkillKey(name, "memory")
                override fun load() = listOf(SkillDefinition(SkillKey(name, "one"), mapOf("schema" to 2, "effects" to listOf(mapOf("type" to "arcana:$type"))), name))
            })
        }
        source("alpha", "native_mark"); source("beta", "native_mark"); source("gamma", "dynamic")
        f.load()
        val one = f.platform.cast(SkillKey("alpha", "one"), f.invocation()).toCompletableFuture()
        val two = f.platform.cast(SkillKey("beta", "one"), f.invocation()).toCompletableFuture()
        val late = f.platform.cast(SkillKey("gamma", "one"), f.invocation()).toCompletableFuture()
        assertTrue(f.platform.reloadSource(SkillKey("alpha", "memory")).success())
        assertEquals(SkillResult.Status.CANCELLED, one.join().status())
        assertFalse(two.isDone); assertFalse(late.isDone)
        f.scheduler.advance(1)
        assertTrue(two.join().isSuccess)
        assertEquals(SkillResult.Status.DEPENDENCY_UNAVAILABLE, late.join().status())
        assertEquals(1, marks)
    }
    @Test fun variableBindingsRefreshBetweenNodesAndBadParametersDoNotConsumeCooldown() {
        val f = PlatformFixture(directory)
        val seen = mutableListOf<Double>()
        f.register("native_mark") { _, _ -> CompiledSkillMechanic { execution ->
            execution.services().require(SkillVariables::class.java).add(SkillVariableScope.INVOCATION, SkillKey("arcana", "counter"), 1.0)
            seen.add(execution.invocation().data().getValue("var_counter").number())
            f.success()
        } }
        f.write(options = "cooldown: 1s\nparameters:\n  power:\n    type: number\nbindings:\n  var_counter:\n    key: counter\n    default: 0", effects = "  - type: native_mark\n  - type: native_mark")
        f.load()
        assertEquals(SkillResult.Status.ERROR, f.cast().join().status())
        assertTrue(f.cast(input = f.invocation().derive().parameters(mapOf("power" to 4)).build()).join().isSuccess)
        assertEquals(listOf(1.0, 2.0), seen)
    }
    @Test fun playerAndMobRootsReadAndApplyOnTheirOwnersAndKeepDistinctRoles() {
        val fixture = PlatformFixture(directory)
        val seen = mutableListOf<SkillInvocation>()
        fixture.register("native_mark") { _, _ -> CompiledSkillMechanic { execution ->
            seen.add(execution.invocation())
            execution.on(execution.invocation().actor()) { entity -> fixture.scheduler.assertOwner(entity); SkillResult.success() }
        } }
        fixture.write(); fixture.load()
        val origin = SkillLocation(fixture.worldId, 100.0, 70.0, 100.0)
        val input = fixture.invocation().derive().target(SkillActor.of(fixture.legacy.victim))
            .trigger(SkillActor.of(fixture.legacy.victim)).origin(origin).build()
        assertTrue(fixture.cast(input = input).join().isSuccess)
        assertEquals(origin, seen[0].origin().orElseThrow())
        assertEquals("Caster", seen[0].data().getValue("caster_name").text())
        assertEquals("Victim", seen[0].data().getValue("target_name").text())
        assertEquals("Victim", seen[0].data().getValue("trigger_name").text())
        assertTrue(fixture.cast(input = fixture.invocation(true)).join().isSuccess)
        assertEquals(2, seen.size)
    }
    @Test fun retiredSelectedTargetsAreRemovedAndCannotReceiveAnAction() {
        val fixture = PlatformFixture(directory)
        every { fixture.legacy.victim.isValid } answers { fixture.scheduler.assertOwner(fixture.legacy.victim); false }
        var applied = false
        fixture.register("native_mark") { _, _ -> CompiledSkillMechanic { execution ->
            assertTrue(execution.invocation().target().isEmpty)
            execution.on(SkillActor.of(fixture.legacy.victim)) { applied = true; SkillResult.success() }
        } }
        fixture.write(); fixture.load()
        val input = fixture.invocation().derive().target(SkillActor.of(fixture.legacy.victim)).build()
        assertEquals(SkillResult.Status.NO_TARGET, fixture.cast(input = input).join().status())
        assertFalse(applied)
    }
    @Test fun rootCancellationAndReloadCancelDelayedNativeWork() {
        val fixture = PlatformFixture(directory)
        var applied = 0
        fixture.register("native_mark") { _, _ -> CompiledSkillMechanic { execution -> execution.delay(20).thenApply { applied++; it } } }
        fixture.write(); fixture.load()
        val pending = fixture.cast()
        fixture.engine.cancel(fixture.legacy.playerId)
        assertEquals(SkillResult.Status.CANCELLED, pending.join().status())
        fixture.scheduler.advance(20)
        assertEquals(0, applied)
        val second = fixture.cast()
        fixture.load()
        assertEquals(SkillResult.Status.CANCELLED, second.join().status())
    }
}
