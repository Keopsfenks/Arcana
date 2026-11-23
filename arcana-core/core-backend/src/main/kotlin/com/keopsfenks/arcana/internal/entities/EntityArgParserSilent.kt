package com.keopsfenks.arcana.internal.entities

import com.keopsfenks.arcana.core.entities.args.EntityArgParseResult
import com.keopsfenks.arcana.core.entities.args.EntityArgParser

object EntityArgParserSilent : EntityArgParser {
    override fun parseArguments(args: Array<out String>): EntityArgParseResult? {
        var silent = false

        for (arg in args) {
            if (arg.equals("silent", true)) {
                silent = true
            }
        }

        if (!silent) {
            return null
        }

        return EntityArgParseResult(
            {
                it.isSilent
            },
            {
                it.isSilent = true
            }
        )
    }
}