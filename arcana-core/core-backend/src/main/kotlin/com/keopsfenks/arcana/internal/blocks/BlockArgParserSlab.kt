package com.keopsfenks.arcana.internal.blocks

import com.keopsfenks.arcana.core.blocks.args.BlockArgParseResult
import com.keopsfenks.arcana.core.blocks.args.BlockArgParser
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.type.Slab

object BlockArgParserSlab : BlockArgParser {
    override fun parseArguments(args: Array<out String>, blockData: BlockData): BlockArgParseResult? {
        if (blockData !is Slab) return null
        var slabType: Slab.Type? = null

        for (arg in args) {
            val argSplit = arg.split(":")
            if (!argSplit[0].equals("slab", true)) {
                continue
            }
            if (argSplit.size < 2) {
                continue
            }
            slabType = runCatching { Slab.Type.valueOf(argSplit[1].uppercase()) }.getOrNull() ?: continue
        }

        slabType ?: return null

        return BlockArgParseResult(
            {
                val slab = it.blockData as? Slab ?: return@BlockArgParseResult false

                slab.type == slabType
            },
            {
                val slab = it.blockData as? Slab ?: return@BlockArgParseResult

                slab.type = slabType
                it.blockData = slab
            }
        )
    }
}