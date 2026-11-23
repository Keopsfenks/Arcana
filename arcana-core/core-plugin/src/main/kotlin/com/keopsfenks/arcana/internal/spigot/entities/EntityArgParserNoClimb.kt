package com.keopsfenks.arcana.internal.spigot.entities

import com.keopsfenks.arcana.core.entities.args.EntityArgParseResult
import com.keopsfenks.arcana.core.entities.args.EntityArgParser
import org.bukkit.entity.Spider

object EntityArgParserNoClimb : EntityArgParser {
    override fun parseArguments(args: Array<out String>): EntityArgParseResult? {
        var noClimb = false

        for (arg in args) {
            if (arg.equals("no-climb", true)) {
                noClimb = true
            }
        }

        if (!noClimb) {
            return null
        }

        return EntityArgParseResult(
            {
                it !is Spider || it.persistentDataContainer.has(SpiderClimbing.key)
            },
            {
                (it as? Spider)?.let { spider -> SpiderClimbing.disable(spider) }
            }
        )
    }
}
