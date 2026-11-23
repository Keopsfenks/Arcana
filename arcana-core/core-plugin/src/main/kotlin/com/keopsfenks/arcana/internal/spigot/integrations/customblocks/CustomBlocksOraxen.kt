package com.keopsfenks.arcana.internal.spigot.integrations.customblocks

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.blocks.Blocks
import com.keopsfenks.arcana.core.blocks.CustomBlock
import com.keopsfenks.arcana.core.blocks.TestableBlock
import com.keopsfenks.arcana.core.blocks.provider.BlockProvider
import com.keopsfenks.arcana.core.integrations.customblocks.CustomBlocksIntegration
import com.keopsfenks.arcana.util.namespacedKeyOf
import io.th0rgal.oraxen.api.OraxenBlocks
import io.th0rgal.oraxen.api.OraxenItems
import io.th0rgal.oraxen.api.events.OraxenItemsLoadedEvent
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class CustomBlocksOraxen(
    private val plugin: ArcanaPlugin
) : CustomBlocksIntegration, Listener {
    override fun registerProvider() {
        plugin.eventManager.registerListener(this)
    }

    override fun getPluginName(): String {
        return "Oraxen"
    }

    @EventHandler
    @Suppress("UNUSED_PARAMETER")
    fun onBlockRegister(event: OraxenItemsLoadedEvent) {
        Blocks.registerBlockProvider(OraxenProvider())
    }

    private class OraxenProvider : BlockProvider("oraxen") {
        override fun provideForKey(key: String): TestableBlock? {
            if (!OraxenBlocks.isOraxenBlock(key)) {
                return null
            }

            val item = OraxenItems.getItemById(key) ?: return null
            val id = OraxenItems.getIdByItem(item)
            val namespacedKey = namespacedKeyOf("oraxen", id)

            val blockData = OraxenBlocks.getOraxenBlockData(id)
            val hardness: Float = if (blockData != null) {
                when (val mechanic = OraxenBlocks.getOraxenBlock(blockData)) {
                    is NoteBlockMechanic -> if (mechanic.hasHardness()) mechanic.hardness.toFloat() else -1f
                    is StringBlockMechanic -> if (mechanic.hasHardness()) mechanic.hardness.toFloat() else -1f
                    else -> -1f
                }
            } else -1f

            return CustomBlock(
                namespacedKey,
                {
                    OraxenBlocks.isOraxenBlock(it) &&
                            id.equals(OraxenBlocks.getOraxenBlock(it.location).itemID, ignoreCase = true)
                },
                { location ->
                    OraxenBlocks.place(id, location)
                    location.block
                },
                hardness
            )
        }
    }
}
