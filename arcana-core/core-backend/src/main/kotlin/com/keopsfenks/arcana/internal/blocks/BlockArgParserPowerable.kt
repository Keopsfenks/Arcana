package com.keopsfenks.arcana.internal.blocks

import com.keopsfenks.arcana.core.blocks.args.BlockArgParseResult
import com.keopsfenks.arcana.core.blocks.args.BlockArgParser
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.Powerable

object BlockArgParserPowerable : BlockArgParser {
    override fun parseArguments(args: Array<out String>, blockData: BlockData): BlockArgParseResult? {
        if (blockData !is Powerable) return null
        var powered: Boolean? = null

        for (arg in args) {
            if (arg.equals("powered", true)) {
                powered = true
            }
        }

        powered ?: return null

        return BlockArgParseResult(
            {
                val powerable = it.blockData as? Powerable ?: return@BlockArgParseResult false

                powerable.isPowered == powered
            },
            {
                val powerable = it.blockData as? Powerable ?: return@BlockArgParseResult

                powerable.isPowered = powered
                it.blockData = powerable
            }
        )
    }
}