package com.keopsfenks.arcana.skills.components.effects.world
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.actions.WorldActions
import com.keopsfenks.arcana.skills.config.ComponentArguments
import org.bukkit.block.BlockFace
import org.bukkit.block.data.BlockData
import org.bukkit.event.Event
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.inventory.EquipmentSlot
import java.util.concurrent.CompletableFuture

class SetBlockMechanic(private val resolve: (String) -> BlockData? = BlockSupport::resolve, private val events: (Event) -> Unit = BlockSupport::event) : SkillMechanic(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("set_block", "Loaded-location block edit obeying cancellable native placement/change events", "target", "block", "physics")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor()); val location = WorldActions.location(arguments)
        val data = resolve(arguments.string("block")) ?: throw arguments.error("block", "unknown block data"); val physics = arguments.bool("physics", false)
        return CompiledSkillMechanic { context ->
            val input = context.invocation(); val point = location(input); val source = input.actor()
            if (point == null) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.NO_TARGET, "Block location missing")) else BlockSupport.hand(context).thenCompose { held ->
                WorldActions.at(context, point, true) { at ->
                    val block = at.world!!.getBlockAt(at.blockX, at.blockY, at.blockZ); val proposed = data.clone()
                    val player = source.player().orElse(null)
                    if (player == null) {
                        val event = EntityChangeBlockEvent(source.entity(), block, proposed); BlockSupport.dispatch(event, events)
                        if (event.isCancelled) SkillResult.of(SkillResult.Status.NO_TARGET, "Block change cancelled")
                        else { context.invocation(); block.setBlockData(proposed, physics); SkillResult.success() }
                    } else {
                        val previous = block.state
                        // The place event exposes the proposed block while its replaced state stays available.
                        block.setBlockData(proposed, false)
                        fun restore() { if (block.blockData == proposed) check(previous.update(true, false)) { "Could not restore cancelled block placement" } }
                        try {
                            val event = BlockPlaceEvent(block, previous, if (at.blockY > at.world!!.minHeight) block.getRelative(BlockFace.DOWN) else block, requireNotNull(held), player, true, EquipmentSlot.HAND)
                            BlockSupport.dispatch(event, events)
                            if (event.isCancelled || !event.canBuild()) { restore(); SkillResult.of(SkillResult.Status.NO_TARGET, "Block placement cancelled") }
                            else { context.invocation(); if (physics && block.blockData == proposed) block.setBlockData(proposed, true); SkillResult.success() }
                        } catch (error: Throwable) { restore(); throw error }
                    }
                }.toCompletableFuture()
            }
        }
    }
}
