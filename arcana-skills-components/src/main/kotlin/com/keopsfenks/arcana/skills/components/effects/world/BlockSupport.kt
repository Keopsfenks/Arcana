package com.keopsfenks.arcana.skills.components.effects.world
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.execution.SkillWorldMutationScope
import org.bukkit.*
import org.bukkit.block.data.BlockData
import org.bukkit.event.Event
import org.bukkit.inventory.ItemStack
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference

internal object BlockSupport {
    fun resolve(key: String): BlockData? = try { Bukkit.createBlockData(key.lowercase(java.util.Locale.ROOT)) } catch (_: IllegalArgumentException) { null }
    fun event(event: Event) { Bukkit.getPluginManager().callEvent(event) }
    fun dispatch(event: Event, dispatcher: (Event) -> Unit) = SkillWorldMutationScope.during { dispatcher(event) }
    fun hand(context: SkillExecutionContext): CompletableFuture<ItemStack?> {
        val actor = context.invocation().actor()
        if (actor.player().isEmpty) return CompletableFuture.completedFuture(null)
        val copy = AtomicReference<ItemStack?>()
        return context.on(actor) { entity -> copy.set((entity as org.bukkit.entity.Player).inventory.itemInMainHand.clone()); SkillResult.success() }
            .thenApply { result -> if (!result.isSuccess) throw SkillProviderException(result); copy.get() }.toCompletableFuture()
    }
}
