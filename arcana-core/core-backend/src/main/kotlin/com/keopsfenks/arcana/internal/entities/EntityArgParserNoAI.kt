package com.keopsfenks.arcana.internal.entities

import com.keopsfenks.arcana.core.entities.args.EntityArgParseResult
import com.keopsfenks.arcana.core.entities.args.EntityArgParser
import org.bukkit.entity.LivingEntity

object EntityArgParserNoAI : EntityArgParser {
    override fun parseArguments(args: Array<out String>): EntityArgParseResult? {
        var noAI = false

        for (arg in args) {
            if (arg.equals("no-ai", true)) {
                noAI = true
            }
        }

        if (!noAI) {
            return null
        }

        return EntityArgParseResult(
            {
                if (it !is LivingEntity) {
                    return@EntityArgParseResult false
                }

                !it.hasAI()
            },
            {
                (it as? LivingEntity)?.setAI(false)
            }
        )
    }
}