package com.keopsfenks.arcana.skills.components.actions
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.compile.NodeOptions
import org.bukkit.entity.LivingEntity
import java.util.Locale
import java.util.concurrent.CompletableFuture

internal object NativeActions {
    fun recipient(args: SkillArguments): (SkillInvocation) -> SkillActor? {
        return when (args.string("target", "selected").lowercase(Locale.ROOT)) {
            "selected" -> { input -> input.target().orElse(input.actor()) }
            "actor", "caster" -> { input -> input.actor() }
            "target" -> { input -> input.target().orElse(null) }
            "trigger" -> { input -> input.trigger().orElse(null) }
            else -> throw args.error("target", "expected selected, actor, target or trigger")
        }
    }
    fun run(context: SkillExecutionContext, recipient: (SkillInvocation) -> SkillActor?, action: (LivingEntity) -> SkillResult): java.util.concurrent.CompletionStage<SkillResult> {
        val actor = recipient(context.invocation()) ?: return CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.NO_TARGET, "Requested actor role is missing"))
        return context.on(actor, action)
    }
    fun number(args: SkillArguments, compiler: SkillCompilation, name: String, fallback: Number, min: Double, max: Double, integer: Boolean = false) = NodeOptions.number(args, compiler, name, fallback, min, max, integer)
}
