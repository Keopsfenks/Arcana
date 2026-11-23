package com.keopsfenks.arcana.internal.entities

import com.keopsfenks.arcana.core.entities.args.EntityArgParseResult
import com.keopsfenks.arcana.core.entities.args.EntityArgParser
import com.keopsfenks.arcana.util.StringUtils

object EntityArgParserName : EntityArgParser {
    override fun parseArguments(args: Array<out String>): EntityArgParseResult? {
        var name: String? = null

        for (arg in args) {
            if (!arg.lowercase().startsWith("name:")) {
                continue
            }
            name = arg.substring(5, arg.length)
        }

        name ?: return null

        val formatted = StringUtils.format(name)

        @Suppress("DEPRECATION")
        return EntityArgParseResult(
            { it.customName == formatted },
            {
                it.isCustomNameVisible = true
                it.customName = formatted
            }
        )
    }
}