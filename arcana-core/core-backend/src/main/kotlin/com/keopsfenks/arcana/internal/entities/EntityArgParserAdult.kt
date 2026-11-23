package com.keopsfenks.arcana.internal.entities

import com.keopsfenks.arcana.core.entities.args.EntityArgParseResult
import com.keopsfenks.arcana.core.entities.args.EntityArgParser
import org.bukkit.entity.Ageable

object EntityArgParserAdult : EntityArgParser {
    override fun parseArguments(args: Array<out String>): EntityArgParseResult? {
        var baby = false

        for (arg in args) {
            if (arg.equals("adult", true)) {
                baby = true
            }
        }

        if (!baby) {
            return null
        }

        return EntityArgParseResult(
            {
                if (it !is Ageable) {
                    return@EntityArgParseResult false
                }

                it.isAdult
            },
            {
                (it as? Ageable)?.setAdult()
            }
        )
    }
}