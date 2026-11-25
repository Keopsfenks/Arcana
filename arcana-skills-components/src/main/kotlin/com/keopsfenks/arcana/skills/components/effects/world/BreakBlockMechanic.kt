package com.keopsfenks.arcana.skills.components.effects.world
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.WorldActions
import com.keopsfenks.arcana.skills.components.actions.NativeMaterials
import com.keopsfenks.arcana.skills.config.ComponentArguments
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.event.Event
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import java.util.concurrent.CompletableFuture

class BreakBlockMechanic(private val air: () -> BlockData = { Material.AIR.createBlockData() }, private val events: (Event) -> Unit = BlockSupport::event) : SkillMechanic(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("break_block", "Loaded-location block removal obeying cancellable native break/change events", "target", "drops", "physics")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor()); val location = WorldActions.location(arguments)
        val empty = air(); val drops = arguments.bool("drops", false); val physics = arguments.bool("physics", false)
        return CompiledSkillMechanic { context ->
            val input = context.invocation(); val point = location(input); val source = input.actor()
            if (point == null) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.NO_TARGET, "Block location missing")) else BlockSupport.hand(context).thenCompose { held ->
                WorldActions.at(context, point, true) { at ->
                    val block = at.world!!.getBlockAt(at.blockX, at.blockY, at.blockZ)
                    if (NativeMaterials.empty(block.type)) SkillResult.of(SkillResult.Status.NO_TARGET, "Block is empty") else {
                        var cancelled: Boolean; var nativeDrops = drops
                        val player = source.player().orElse(null)
                        if (player != null) {
                            val event = BlockBreakEvent(block, player); event.isDropItems = drops; BlockSupport.dispatch(event, events)
                            cancelled = event.isCancelled; nativeDrops = event.isDropItems
                        } else {
                            val event = EntityChangeBlockEvent(source.entity(), block, empty.clone()); BlockSupport.dispatch(event, events); cancelled = event.isCancelled
                        }
                        if (cancelled) SkillResult.of(SkillResult.Status.NO_TARGET, "Block break cancelled") else {
                            context.invocation()
                            if (nativeDrops) {
                                val broken = com.keopsfenks.arcana.skills.execution.SkillWorldMutationScope.during { if (held == null) block.breakNaturally() else block.breakNaturally(held) }
                                if (!broken) SkillResult.of(SkillResult.Status.NO_TARGET, "Native block break failed") else SkillResult.success()
                            } else { block.setBlockData(empty.clone(), physics); SkillResult.success() }
                        }
                    }
                }.toCompletableFuture()
            }
        }
    }
}
