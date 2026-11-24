package com.keopsfenks.arcana.skills.platform.admission

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.execution.PlatformFixture
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

class AdmissionTests {
    @TempDir lateinit var directory: Path
    private class Reservation(val outcome: SkillResult = SkillResult.success()) : SkillResourceReservation {
        val commits = AtomicInteger(); val releases = AtomicInteger()
        override fun result() = outcome
        override fun commit() { commits.incrementAndGet() }
        override fun release() { releases.incrementAndGet() }
    }
    private fun fixture() = PlatformFixture(directory).also { f -> f.register("native_mark") { _, _ -> CompiledSkillMechanic { f.legacy.marks.add("mark"); f.success() } } }
    private fun provider(f: PlatformFixture, name: String, reserve: (Double) -> SkillResourceReservation) = f.platform.register(f.legacy.plugin, SkillComponentType.RESOURCE, SkillKey("arcana", name), object : SkillResourceProvider {
        override fun descriptor() = SkillDescriptor.of(name, "test")
        override fun reserve(actor: SkillActor, amount: Double) = CompletableFuture.completedFuture(reserve(amount))
    })
    @Test fun conditionsAndChanceRejectBeforeReservingAndEveryCountsOnlyQualifyingAttempts() {
        val f = fixture(); val calls = AtomicInteger()
        provider(f, "first") { calls.incrementAndGet(); Reservation() }
        f.platform.register(f.legacy.plugin, SkillComponentType.PREDICATE, SkillKey("arcana", "never"), object : SkillPredicateType(SkillDescriptor.of("never", "test")) {
            override fun compile(arguments: SkillArguments, compilation: SkillCompilation) = SkillPredicate { false }
        })
        f.write(options = "conditions: [{type: never}]\ncosts:\n  - {resource: first, amount: 2}")
        f.load()
        assertEquals(SkillResult.Status.CONDITION_FAILED, f.cast().join().status()); assertEquals(0, calls.get())
        f.write(options = "chance: 50\nevery: 2\ncosts:\n  - resource: first\n    amount: 2")
        f.load(); f.legacy.roll = .99
        assertEquals(SkillResult.Status.CHANCE_FAILED, f.cast().join().status()); assertEquals(0, calls.get())
        f.legacy.roll = 0.0
        assertEquals(SkillResult.Status.CONDITION_FAILED, f.cast().join().status()); assertEquals(0, calls.get())
        assertTrue(f.cast().join().isSuccess); assertEquals(1, calls.get())
    }
    @Test fun laterReserveFailureReleasesEarlierReservationsAndDoesNotConsumeCooldown() {
        val f = fixture(); val first = mutableListOf<Reservation>(); var enough = false
        provider(f, "first") { Reservation().also(first::add) }
        provider(f, "second") { Reservation(if (enough) SkillResult.success() else SkillResult.of(SkillResult.Status.INSUFFICIENT_RESOURCE, "not enough")) }
        f.write(options = "cooldown: 1s\ncosts:\n  - {resource: first, amount: 2}\n  - {resource: second, amount: 3}")
        f.load()
        assertEquals(SkillResult.Status.INSUFFICIENT_RESOURCE, f.cast().join().status())
        assertEquals(0, first[0].commits.get()); assertEquals(1, first[0].releases.get())
        enough = true; assertTrue(f.cast().join().isSuccess)
        assertEquals(1, first[1].commits.get()); assertEquals(0, first[1].releases.get())
        assertEquals(SkillResult.Status.COOLDOWN, f.cast().join().status())
        assertEquals(1, first[2].releases.get())
    }
    @Test fun nodeRepeatsReuseOneAdmissionAndEffectErrorsKeepCommittedCosts() {
        val f = fixture(); val reservations = mutableListOf<Reservation>(); val ids = mutableListOf<java.util.UUID>()
        provider(f, "first") { Reservation().also(reservations::add) }
        f.register("admission_mark") { _, _ -> CompiledSkillMechanic { execution ->
            val admission = execution.services().require(SkillAdmission::class.java)
            assertTrue(admission.committed()); ids.add(admission.id()); f.success()
        } }
        f.register("fail") { _, _ -> CompiledSkillMechanic { CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.ERROR, "expected")) } }
        f.write(options = "costs:\n  - {resource: first, amount: 2}", effects = "  - type: admission_mark\n    repeat: 3\n  - type: fail")
        f.load()
        assertEquals(SkillResult.Status.ERROR, f.cast().join().status())
        assertEquals(3, ids.size); assertEquals(1, ids.distinct().size)
        assertEquals(1, reservations.single().commits.get()); assertEquals(0, reservations.single().releases.get())
    }
    @Test fun anAlreadyAdmittedContinuationReusesTheExactTokenAndCannotForgeOrReuseAnExpiredToken() {
        val f = fixture(); val reservations = mutableListOf<Reservation>()
        provider(f, "first") { Reservation().also(reservations::add) }
        var token: SkillAdmission? = null
        var execution: com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionContext? = null
        f.register("capture") { _, _ -> CompiledSkillMechanic {
            execution = it as com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionContext
            token = it.services().require(SkillAdmission::class.java)
            it.delay(10)
        } }
        f.write(options = "costs:\n  - {resource: first, amount: 2}", effects = "  - type: capture")
        f.load(); val pending = f.cast()
        val context = execution!!; val accepted = token!!
        val revision = context.state.snapshot.entries.getValue(context.definition.key)
        val coordinator = context.platform.admissions
        val reused = coordinator.reuse(accepted, revision, context.invocation().actor())
        assertTrue(reused.result.isSuccess); assertSame(accepted, reused.admission)
        assertEquals(1, reservations.size); assertEquals(1, reservations.single().commits.get())
        val forged = object : SkillAdmission {
            override fun id() = accepted.id()
            override fun skill() = accepted.skill()
            override fun actor() = accepted.actor()
            override fun committed() = true
        }
        assertFalse(coordinator.reuse(forged, revision, context.invocation().actor()).result.isSuccess)
        f.engine.cancel(f.legacy.playerId)
        assertEquals(SkillResult.Status.CANCELLED, pending.join().status())
        assertFalse(coordinator.reuse(accepted, revision, accepted.actor()).result.isSuccess)
    }
    @Test fun conflictingChargeGroupDefinitionsFailPreflightWithoutReplacingPublishedDefinitions() {
        val f = fixture()
        f.write("one", options = "charges:\n  max: 2\n  refill: 1s\n  group: shared")
        f.load()
        f.write("two", options = "charges:\n  max: 3\n  refill: 1s\n  group: shared")
        val invalid = f.engine.reload()
        assertFalse(invalid.success())
        assertEquals(1, f.platform.definitions().size)
    }
    @Test fun chargesRefillWithTheInjectedMonotonicClockAndMissingProvidersFailPreflight() {
        val f = fixture()
        f.write(options = "charges:\n  max: 1\n  refill: 1s")
        f.load(); assertTrue(f.cast().join().isSuccess)
        assertEquals(SkillResult.Status.COOLDOWN, f.cast().join().status())
        f.legacy.clock.set(1_000_000_000)
        assertTrue(f.cast().join().isSuccess)
        f.write(options = "costs:\n  - {resource: absent:mana, amount: 2}")
        val invalid = f.engine.reload()
        assertFalse(invalid.success()); assertTrue(invalid.errors().any { it.contains("absent:mana") })
    }
}
