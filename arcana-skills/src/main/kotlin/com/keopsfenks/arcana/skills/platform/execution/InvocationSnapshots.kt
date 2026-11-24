package com.keopsfenks.arcana.skills.platform.execution

import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.execution.SkillScheduler
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import java.util.concurrent.*
import com.keopsfenks.arcana.skills.platform.values.InvocationFrame

object InvocationSnapshots {
    fun valid(entity: LivingEntity) = if (entity is Player) entity.isOnline else entity.isValid

    @Suppress("DEPRECATION")
    fun actor(reference: SkillActor, scheduler: SkillScheduler, state: PlatformExecutionState, frame: InvocationFrame? = null): CompletableFuture<Map<String, SkillValue>?> =
        scheduler.on(reference.entity()) {
            state.checkActive(); frame?.checkActive()
            val entity = reference.entity()
            if (!valid(entity)) null else {
                val location = entity.location
                val vector = entity.velocity
                val values = linkedMapOf<String, Any>("name" to entity.name, "type" to entity.type.name,
                    "health" to entity.health, "max_health" to entity.maxHealth,
                    "location" to SkillLocation(requireNotNull(location.world).uid, location.x, location.y, location.z, location.yaw, location.pitch),
                    "velocity" to SkillVector(vector.x, vector.y, vector.z), "player" to (entity is Player),
                    "world" to entity.world.name)
                if (entity is Player) {
                    values["food"] = entity.foodLevel; values["level"] = entity.level
                    values["sneaking"] = entity.isSneaking; values["sprinting"] = entity.isSprinting
                    values["item"] = entity.inventory.itemInMainHand.type.name
                }
                SkillValue.of(values).map()
            }
        }.let { state.track(frame?.track(it) ?: it) }.handle { value, error ->
            state.checkActive(); frame?.checkActive()
            if (error == null) value else {
                val failure = PlatformExecutionState.failure(error)
                if (failure.status() == com.keopsfenks.arcana.core.skills.SkillResult.Status.CANCELLED) null else throw CompletionException(error)
            }
        }

    fun capture(input: SkillInvocation, scheduler: SkillScheduler, state: PlatformExecutionState, frame: InvocationFrame? = null): CompletableFuture<SkillInvocation> {
        val references = (listOf(input.actor()) + listOfNotNull(input.target().orElse(null), input.trigger().orElse(null)) + input.selection().entities()).distinctBy { it.id() }
        val snapshots = references.associateWith { actor(it, scheduler, state, frame) }
        return CompletableFuture.allOf(*snapshots.values.toTypedArray()).thenApply {
            state.checkActive(); frame?.checkActive()
            val caster = snapshots.getValue(input.actor()).join() ?: throw CancellationException("Actor is unavailable")
            val values = LinkedHashMap(input.data())
            // Trusted snapshots replace caller-supplied values, including stale selected-role fields.
            values.keys.removeIf { it.startsWith("caster_") || it.startsWith("actor_") || it.startsWith("target_") || it.startsWith("trigger_") || it in setOf("world", "item", "entity") }
            fun copy(prefix: String, snapshot: Map<String, SkillValue>?) { snapshot?.forEach { (key, value) -> values["${prefix}_$key"] = value } }
            copy("caster", caster); copy("actor", caster)
            caster["world"]?.let { values["world"] = it }; caster["item"]?.let { values["item"] = it }
            val target = input.target().orElse(null)?.takeIf { snapshots[it]?.join() != null }
            val trigger = input.trigger().orElse(null)?.takeIf { snapshots[it]?.join() != null }
            copy("target", target?.let { snapshots[it]?.join() }); copy("trigger", trigger?.let { snapshots[it]?.join() })
            target?.let { snapshots[it]?.join()?.get("type")?.let { type -> values["entity"] = type } }
            val location = caster.getValue("location").location()
            input.derive().replaceData(values).target(target).trigger(trigger)
                .origin(input.origin().orElse(location)).location(input.location().orElse(location))
                .selection(SkillSelection(input.selection().entities().filter { snapshots[it]?.join() != null }, input.selection().locations())).build()
        }
    }
}
