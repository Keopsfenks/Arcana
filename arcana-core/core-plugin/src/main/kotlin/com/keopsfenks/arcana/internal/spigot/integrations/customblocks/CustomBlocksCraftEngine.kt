package com.keopsfenks.arcana.internal.spigot.integrations.customblocks

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.blocks.Blocks
import com.keopsfenks.arcana.core.blocks.CustomBlock
import com.keopsfenks.arcana.core.blocks.TestableBlock
import com.keopsfenks.arcana.core.blocks.provider.BlockProvider
import com.keopsfenks.arcana.core.integrations.customblocks.CustomBlocksIntegration
import com.keopsfenks.arcana.util.namespacedKeyOf
import java.util.function.Predicate
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks
import net.momirealms.craftengine.bukkit.api.event.CraftEngineReloadEvent
import net.momirealms.craftengine.core.util.Key
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class CustomBlocksCraftEngine(
    private val plugin: ArcanaPlugin
) : CustomBlocksIntegration, Listener {
    override fun registerProvider() {
        plugin.eventManager.registerListener(this)
    }

    override fun getPluginName(): String {
        return "CraftEngine"
    }

    @EventHandler
    @Suppress("UNUSED_PARAMETER")
    fun onBlockRegister(event: CraftEngineReloadEvent) {
        Blocks.registerBlockProvider(CraftEngineProvider())
    }

    private class CraftEngineProvider : BlockProvider("craftengine") {
        override fun provideForKey(key: String): TestableBlock? {
            val split = key.split(":").toMutableList()

            if (split.size < 2) {
                return null
            }

            val namespace = split[0]
            val value = split[1]

            val id = Key.of(namespace, value)
            val blockId = CraftEngineBlocks.byId(id) ?: return null
            val namespacedKey = namespacedKeyOf("craftengine", key.lowercase().replace(":", "__"))
            val hardness = blockId.defaultState().settings().hardness()

            return CustomBlock(
                namespacedKey,
                Predicate { test: Block ->
                    if (!CraftEngineBlocks.isCustomBlock(test)) {
                        return@Predicate false
                    }
                    val immutableBlockState = CraftEngineBlocks.getCustomBlockState(test) ?: return@Predicate false
                    return@Predicate immutableBlockState.owner().value().id().equals(blockId.id())
                },
                { location ->
                    CraftEngineBlocks.place(location, id, true)
                    location.block
                },
                hardness
            )
        }

    }
}