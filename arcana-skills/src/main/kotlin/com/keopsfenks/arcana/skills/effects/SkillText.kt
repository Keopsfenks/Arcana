package com.keopsfenks.arcana.skills.effects

import com.keopsfenks.arcana.core.skills.SkillContext
import com.keopsfenks.arcana.util.StringUtils
import net.kyori.adventure.text.Component
import java.util.regex.Pattern

object SkillText {
    private val tokens = Pattern.compile("\\{(player|target|skill)}")

    fun render(text: String, context: SkillContext, parser: (String) -> Component = StringUtils::formatToRichComponent): Component {
        val values = mapOf("player" to context.getText("caster_name"), "target" to context.getText("target_name"), "skill" to context.getText("skill"))
        return parser(text).replaceText { builder ->
            builder.match(tokens).replacement { match, _ -> Component.text(values[match.group(1)].orEmpty()) }
        }
    }
}
