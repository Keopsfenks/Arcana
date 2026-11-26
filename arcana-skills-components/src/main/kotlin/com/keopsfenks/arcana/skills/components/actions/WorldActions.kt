package com.keopsfenks.arcana.skills.components.actions
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionContext
import com.keopsfenks.arcana.skills.platform.targets.TargetGeometry
import org.bukkit.Location

internal object WorldActions {
    fun location(args: SkillArguments): (SkillInvocation) -> SkillLocation? {
        if (!args.has("target")) return { TargetGeometry.center(it) }
        val recipient = NativeActions.recipient(args)
        return { input -> recipient(input)?.let { actor ->
            val prefix = when (actor) { input.actor() -> "actor"; input.target().orElse(null) -> "target"; else -> "trigger" }
            input.data()["${prefix}_location"]?.location()
        } }
    }
    fun at(context: SkillExecutionContext, location: SkillLocation, height: Boolean = false, action: (Location) -> SkillResult): java.util.concurrent.CompletionStage<SkillResult> = context.at(location) { at ->
        val world = at.world
        if (world == null || !world.isChunkLoaded(at.blockX shr 4, at.blockZ shr 4) || height && at.blockY !in world.minHeight until world.maxHeight) SkillResult.of(SkillResult.Status.NO_TARGET, "Location is unloaded or outside world bounds")
        else { (context as? PlatformExecutionContext)?.state?.step(); action(at) }
    }
}
