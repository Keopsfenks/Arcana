package com.keopsfenks.arcana.skills.components.effects.movement
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.NativeActions
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.execution.ScheduledWork
import com.keopsfenks.arcana.skills.platform.execution.*
import org.bukkit.Location
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference

class TeleportMechanic : SkillMechanic(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("teleport", "Await the server's ownership-safe asynchronous teleport result", "target", "destination")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor()); val recipient = NativeActions.recipient(arguments)
        val destination = arguments.section("destination")
        destination.values.keys.firstOrNull { it !in setOf("world", "x", "y", "z", "yaw", "pitch") }?.let { throw destination.error(it, "unknown destination field") }
        for (coordinate in listOf("x", "y", "z")) require(destination.has(coordinate)) { "${destination.path}.$coordinate: required" }
        val world = if (destination.has("world")) try { UUID.fromString(destination.string("world")) } catch (_: IllegalArgumentException) { throw destination.error("world", "expected a world UUID") } else null
        val x = NativeActions.number(destination, compilation, "x", 0, -30_000_000.0, 30_000_000.0); val y = NativeActions.number(destination, compilation, "y", 0, -2048.0, 2048.0); val z = NativeActions.number(destination, compilation, "z", 0, -30_000_000.0, 30_000_000.0)
        val yaw = NativeActions.number(destination, compilation, "yaw", 0, -360.0, 360.0); val pitch = NativeActions.number(destination, compilation, "pitch", 0, -90.0, 90.0)
        return CompiledSkillMechanic { context ->
            val input = context.invocation(); val actor = recipient(input)
            if (actor == null) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.NO_TARGET, "Teleport target missing"))
            else if (context !is PlatformExecutionContext) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.UNSUPPORTED_CONTEXT, "Teleport requires Arcana execution"))
            else {
                val point = SkillLocation(world ?: input.location().orElseThrow().world(), x.evaluate(input), y.evaluate(input), z.evaluate(input), yaw.evaluate(input).toFloat(), pitch.evaluate(input).toFloat())
                val resolved = AtomicReference<Location>(); val pending = AtomicReference<CompletableFuture<Boolean>>()
                context.at(point) { resolved.set(it.clone()); SkillResult.success() }.thenCompose { outcome ->
                    if (!outcome.isSuccess) CompletableFuture.completedFuture(outcome)
                    else context.on(actor) { entity -> pending.set(entity.teleportAsync(resolved.get())); SkillResult.success() }
                }.thenCompose { outcome ->
                    if (!outcome.isSuccess) CompletableFuture.completedFuture(outcome)
                    else { val future = pending.get(); context.track(ScheduledWork(future) { future.cancel(false) }).handle { success, error ->
                        context.state.checkActive(); context.frame.checkActive()
                        if (error != null) PlatformExecutionState.failure(error) else if (success) SkillResult.success() else SkillResult.of(SkillResult.Status.NO_TARGET, "Teleport cancelled or failed")
                    } }
                }
            }
        }
    }
}
