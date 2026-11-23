package com.keopsfenks.arcana.skills.effects

import com.keopsfenks.arcana.core.skills.SkillContext
import io.mockk.mockk
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SkillTextTests {
    @Test fun `placeholder values become literal text instead of reparsed MiniMessage tags`() {
        val malicious = "<click:run_command:'/op me'>name</click>"
        val context = SkillContext.builder(mockk<Player>()).value("target_name", malicious).build()
        val rendered = SkillText.render("<red>{target}</red>", context) { MiniMessage.miniMessage().deserialize(it) }
        fun flatten(component: Component): String {
            assertNull(component.clickEvent())
            return (if (component is TextComponent) component.content() else "") + component.children().joinToString("") { flatten(it) }
        }
        assertEquals(malicious, flatten(rendered))
        val nested = SkillContext.builder(mockk<Player>()).value("target_name", "{player}").value("caster_name", "Other").build()
        assertEquals("{player}", flatten(SkillText.render("{target}", nested) { Component.text(it) }))
    }
}
