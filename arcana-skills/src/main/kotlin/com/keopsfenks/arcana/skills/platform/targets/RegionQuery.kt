package com.keopsfenks.arcana.skills.platform.targets

import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.execution.*
import com.keopsfenks.arcana.skills.platform.execution.PlatformBudgetException
import com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionState
import com.keopsfenks.arcana.skills.platform.execution.InvocationSnapshots
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.LivingEntity
import org.bukkit.util.BoundingBox
import java.util.*
import java.util.concurrent.*
import kotlin.math.*
import com.keopsfenks.arcana.skills.platform.values.InvocationFrame

/** Resolve references globally, enumerate each loaded chunk locally, snapshot each entity on its owner. */
class RegionQuery(
    private val scheduler: SkillScheduler,
    private val worlds: (UUID) -> World? = { Bukkit.getWorld(it) },
    private val state: PlatformExecutionState? = null,
    private val frame: InvocationFrame? = null
) : SkillTargetQuery {
    private fun check() { state?.checkActive(); frame?.checkActive() }
    private fun <T> track(work: ScheduledWork<T>): CompletableFuture<T> = (frame?.track(work) ?: work).let { state?.track(it) ?: it.future }
    private fun world(id: UUID) = track(scheduler.global { check(); worlds(id) })
    private fun chunks(bounds: SkillBounds): List<Pair<Int, Int>> {
        val x1 = floor(bounds.minX()).toInt() shr 4; val x2 = floor(bounds.maxX()).toInt() shr 4
        val z1 = floor(bounds.minZ()).toInt() shr 4; val z2 = floor(bounds.maxZ()).toInt() shr 4
        if ((x2.toLong() - x1 + 1) * (z2.toLong() - z1 + 1) > 256) throw PlatformBudgetException("Query exceeds 256 chunks")
        return (x1..x2).flatMap { x -> (z1..z2).map { z -> x to z } }
    }
    private fun <T> onChunks(world: World, bounds: SkillBounds, action: (Int, Int) -> List<T>): CompletableFuture<List<T>> {
        var result = CompletableFuture.completedFuture(emptyList<T>())
        for ((x, z) in chunks(bounds)) result = result.thenCompose { previous ->
            val location = Location(world, x * 16.0, 0.0, z * 16.0)
            track(scheduler.at(location) { check(); if (!world.isChunkLoaded(x, z)) emptyList() else action(x, z) })
                .thenApply { next ->
                    if (previous.size + next.size > 4096) throw PlatformBudgetException("Query exceeds 4096 candidates")
                    previous + next
                }
        }
        return result
    }
    override fun entities(bounds: SkillBounds): CompletionStage<List<SkillActor>> {
        chunks(bounds) // Validate before scheduling any world work.
        return world(bounds.world()).thenCompose { world ->
            if (world == null) CompletableFuture.completedFuture(emptyList()) else onChunks(world, bounds) { x, z ->
                world.getChunkAt(x, z).entities.filterIsInstance<LivingEntity>().map(SkillActor::of)
            }.thenCompose { candidates ->
                // Sequential bounded snapshots avoid scheduling thousands of foreign callbacks together.
                var selected = CompletableFuture.completedFuture(emptyList<SkillActor>())
                for (candidate in candidates.distinctBy { it.id() }) selected = selected.thenCompose { prior ->
                    snapshot(candidate).thenApply { captured ->
                        if (captured.isPresent && bounds.intersects(captured.get().bounds())) prior + candidate else prior
                    }
                }
                selected.thenApply { java.util.List.copyOf(it) }
            }
        }
    }
    override fun loaded(bounds: SkillBounds): CompletionStage<Boolean> {
        val partitions = chunks(bounds)
        return world(bounds.world()).thenCompose { world ->
            if (world == null) CompletableFuture.completedFuture(false) else {
                var result = CompletableFuture.completedFuture(true)
                for ((x, z) in partitions) result = result.thenCompose { previous ->
                    if (!previous) CompletableFuture.completedFuture(false)
                    else track(scheduler.at(Location(world, x * 16.0, 0.0, z * 16.0)) { check(); world.isChunkLoaded(x, z) })
                }
                result
            }
        }
    }
    override fun snapshot(actor: SkillActor): CompletionStage<Optional<SkillEntitySnapshot>> =
        track(scheduler.on(actor.entity()) {
            check()
            val entity = actor.entity()
            if (!InvocationSnapshots.valid(entity)) Optional.empty() else {
                val location = entity.location
                val box = entity.boundingBox
                val id = requireNotNull(location.world).uid
                Optional.of(SkillEntitySnapshot(actor, SkillLocation(id, location.x, location.y, location.z, location.yaw, location.pitch),
                    SkillBounds(id, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ),
                    SkillValue.of(mapOf("type" to entity.type.name, "player" to actor.player().isPresent, "health" to entity.health)).map()))
            }
        }).handle { value, error ->
            check()
            if (error == null) value else {
                val failure = PlatformExecutionState.failure(error)
                if (failure.status() == com.keopsfenks.arcana.core.skills.SkillResult.Status.CANCELLED) Optional.empty() else throw CompletionException(error)
            }
        }
    override fun collisions(from: SkillLocation, to: SkillLocation, radius: Double): CompletionStage<List<SkillCollision>> {
        require(from.world() == to.world()) { "Collision segment crosses worlds" }
        require(radius.isFinite() && radius in 0.0..16.0) { "Collision radius must be between zero and 16" }
        val bounds = SkillBounds(from.world(), min(from.x(), to.x()) - radius, min(from.y(), to.y()) - radius, min(from.z(), to.z()) - radius,
            max(from.x(), to.x()) + radius, max(from.y(), to.y()) + radius, max(from.z(), to.z()) + radius)
        chunks(bounds)
        val volume = (floor(bounds.maxX()).toLong() - floor(bounds.minX()).toLong() + 1) *
            (floor(bounds.maxY()).toLong() - floor(bounds.minY()).toLong() + 1) * (floor(bounds.maxZ()).toLong() - floor(bounds.minZ()).toLong() + 1)
        if (volume > 65_536) throw PlatformBudgetException("Collision query exceeds 65536 blocks")
        return world(from.world()).thenCompose { world ->
            if (world == null) CompletableFuture.completedFuture(emptyList()) else onChunks(world, bounds) { chunkX, chunkZ ->
                val found = mutableListOf<SkillCollision>()
                for (x in max(floor(bounds.minX()).toInt(), chunkX * 16)..min(floor(bounds.maxX()).toInt(), chunkX * 16 + 15))
                    for (z in max(floor(bounds.minZ()).toInt(), chunkZ * 16)..min(floor(bounds.maxZ()).toInt(), chunkZ * 16 + 15))
                        for (y in max(floor(bounds.minY()).toInt(), world.minHeight)..min(floor(bounds.maxY()).toInt(), world.maxHeight - 1)) {
                            val block = world.getBlockAt(x, y, z)
                            for (shape in block.collisionShape.boundingBoxes) intersect(from, to, shape.clone().shift(x.toDouble(), y.toDouble(), z.toDouble()), radius, null)?.let(found::add)
                        }
                found
            }.thenCompose { blocks ->
                entities(bounds).thenCompose { candidates ->
                    var hits = CompletableFuture.completedFuture(blocks)
                    for (candidate in candidates) hits = hits.thenCompose { previous -> snapshot(candidate).thenApply { value ->
                        if (value.isEmpty) previous else value.get().bounds().let { box ->
                            val hit = intersect(from, to, BoundingBox(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()), radius, candidate)
                            if (hit == null) previous else previous + hit
                        }
                    } }
                    hits.thenApply { results -> java.util.List.copyOf(results.distinctBy { Triple(it.actor().orElse(null)?.id(), it.location(), it.normal()) }
                        .sortedWith(compareBy<SkillCollision> { it.fraction() }.thenBy { it.actor().map { actor -> actor.id().toString() }.orElse("") })) }
                }
            }
        }
    }
    companion object {
        internal fun intersect(from: SkillLocation, to: SkillLocation, box: BoundingBox, radius: Double, actor: SkillActor?): SkillCollision? {
            val start = doubleArrayOf(from.x(), from.y(), from.z()); val end = doubleArrayOf(to.x(), to.y(), to.z())
            val low = doubleArrayOf(box.minX - radius, box.minY - radius, box.minZ - radius)
            val high = doubleArrayOf(box.maxX + radius, box.maxY + radius, box.maxZ + radius)
            var enter = 0.0; var exit = 1.0; var normal = SkillVector(0.0, 0.0, 0.0)
            for (axis in 0..2) {
                val delta = end[axis] - start[axis]
                if (abs(delta) < 1e-12) { if (start[axis] < low[axis] || start[axis] > high[axis]) return null; continue }
                var first = (low[axis] - start[axis]) / delta; var last = (high[axis] - start[axis]) / delta
                if (first > last) { val swap = first; first = last; last = swap }
                if (first > enter) {
                    enter = first
                    val face = if (delta > 0) -1.0 else 1.0
                    normal = SkillVector(if (axis == 0) face else 0.0, if (axis == 1) face else 0.0, if (axis == 2) face else 0.0)
                }
                exit = min(exit, last)
                if (enter > exit) return null
            }
            if (enter !in 0.0..1.0) return null
            return SkillCollision(SkillLocation(from.world(), start[0] + (end[0] - start[0]) * enter, start[1] + (end[1] - start[1]) * enter,
                start[2] + (end[2] - start[2]) * enter), normal, Optional.ofNullable(actor), enter)
        }
    }
}
