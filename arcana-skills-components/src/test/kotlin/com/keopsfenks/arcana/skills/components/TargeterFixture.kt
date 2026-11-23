package com.keopsfenks.arcana.skills.components

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import io.mockk.*
import org.bukkit.entity.LivingEntity
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.DoubleSupplier

class TargeterFixture {
    val world = UUID.randomUUID()
    val origin = SkillLocation(world, 0.0, 0.0, 0.0)
    val snapshots = mutableListOf<SkillEntitySnapshot>()
    val self = entity(0.0, 0.0, player = true)
    val victim = entity(3.0, 0.0)
    val trigger = entity(5.0, 0.0)
    var invocation = SkillInvocation.builder(self.actor()).target(victim.actor()).trigger(trigger.actor()).origin(origin).location(origin).build()
    var hits = emptyList<SkillCollision>()
    var queries = 0
    val query = object : SkillTargetQuery {
        override fun entities(bounds: SkillBounds) = CompletableFuture.completedFuture(snapshots.filter { bounds.intersects(it.bounds()) }.map { it.actor() }).also { queries++ }
        override fun snapshot(actor: SkillActor) = CompletableFuture.completedFuture(Optional.ofNullable(snapshots.find { it.actor() == actor }))
        override fun collisions(from: SkillLocation, to: SkillLocation, radius: Double) = CompletableFuture.completedFuture(hits)
    }
    val compiler = mockk<SkillCompilation>()
    init {
        every { compiler.namespace() } returns "arcana"
        every { compiler.number(any(), any()) } answers {
            val value = firstArg<Any>()
            check(value is Number) { "Unexpected test expression: $value" }
            SkillNumericExpression { value.toDouble() }
        }
        every { compiler.predicates(any(), any()) } returns SkillPredicate { true }
    }
    fun entity(x: Double, z: Double, y: Double = 0.0, player: Boolean = false): SkillEntitySnapshot {
        val reference = mockk<LivingEntity>()
        every { reference.uniqueId } returns UUID.randomUUID()
        return SkillEntitySnapshot(SkillActor.of(reference), SkillLocation(world, x, y, z), SkillBounds(world, x, y, z, x, y, z),
            SkillValue.of(mapOf("player" to player, "type" to if (player) "PLAYER" else "ZOMBIE", "health" to 20)).map()).also(snapshots::add)
    }
    fun select(targeter: SkillTargeter, values: Map<String, Any> = emptyMap(), seed: Long = 1): SkillSelection {
        val random = Random(seed)
        return targeter.compile(SkillArguments(values, "targeter"), compiler).select(SkillTargetingContext(invocation, query, DoubleSupplier { random.nextDouble() })).toCompletableFuture().join()
    }
}
