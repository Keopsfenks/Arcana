package com.keopsfenks.arcana.skills.platform.managed
import com.keopsfenks.arcana.core.skills.SkillResult
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.execution.SkillScheduler
import com.keopsfenks.arcana.skills.platform.execution.InvocationSnapshots
import com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionState
import java.util.function.Function
import org.bukkit.entity.LivingEntity
/** Independent of the closed source frame; scheduling always uses the still-running host plugin. */
class DefaultSkillCleanup(private val scheduler: SkillScheduler) : SkillCleanup {
    override fun on(actor: SkillActor, cleanup: Function<LivingEntity, SkillResult>) = scheduler.on(actor.entity()) {
        if (!InvocationSnapshots.valid(actor.entity())) SkillResult.of(SkillResult.Status.NO_TARGET, "Cleanup entity retired")
        else requireNotNull(cleanup.apply(actor.entity()))
    }.future.handle { value, error -> if (error == null) value else PlatformExecutionState.failure(error) }
}
