package com.keopsfenks.arcana.skills.components.effects.status
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionContext
import com.keopsfenks.arcana.skills.platform.managed.ManagedExecutionState
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.potion.PotionEffectType
import java.util.Locale
import java.util.concurrent.CompletableFuture

internal object StatusSupport {
    fun attribute(name: String): Attribute? = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(name))
    fun potion(name: String): PotionEffectType? = Registry.EFFECT.get(requireNotNull(NamespacedKey.fromString(if (':' in name) name else "minecraft:$name")))
    fun type(args: SkillArguments, resolver: (String) -> PotionEffectType?): PotionEffectType = resolver(args.string("potion").lowercase(Locale.ROOT)) ?: throw args.error("potion", "unknown potion type")
    fun owned(context: SkillExecutionContext, actor: SkillActor, contribution: CompiledSkillContribution): CompletableFuture<SkillResult> {
        if (context !is PlatformExecutionContext) return CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.UNSUPPORTED_CONTEXT, "Status requires managed Arcana execution"))
        val managed = context.services().require(ManagedExecutionState::class.java)
        return context.derived(context.invocation().derive().actor(actor).target(null).selection(SkillSelection.empty()).build()).thenCompose { recipient ->
            recipient.platform.contributions.apply(managed.revision, recipient, contribution).thenApply { lease -> managed.own(lease); SkillResult.success() }
                .whenComplete { _, _ -> recipient.state.release(recipient.frame) }
        }
    }
}
