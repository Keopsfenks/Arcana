package com.keopsfenks.arcana.skills.filters

import com.keopsfenks.arcana.core.skills.CompiledSkillCondition
import com.keopsfenks.arcana.core.skills.SkillArguments
import org.bukkit.Material
import org.bukkit.entity.EntityType
import java.util.Locale

object SkillFilters {
    fun compile(arguments: SkillArguments): CompiledSkillCondition {
        val fields = mapOf("worlds" to "world", "blocks" to "block", "entities" to "entity", "items" to "item")
        arguments.values.keys.firstOrNull { it !in fields }?.let { throw arguments.error(it, "unknown filter") }
        val compiled = fields.mapNotNull { (field, valueKey) ->
            if (!arguments.has(field)) return@mapNotNull null
            val expected = arguments.list(field).mapIndexed { index, value ->
                require(value is String && value.isNotBlank()) { "${arguments.path}.$field[$index]: expected a non-empty string" }
                if (field == "worlds") value else {
                    val name = value.uppercase(Locale.ROOT)
                    try {
                        if (field == "entities") EntityType.valueOf(name) else Material.valueOf(name)
                    } catch (_: IllegalArgumentException) { throw arguments.error("$field[$index]", "unknown ${if (field == "entities") "entity type" else "material"} $value") }
                    name
                }
            }.toSet()
            valueKey to expected
        }
        return CompiledSkillCondition { context -> compiled.all { (key, values) -> values.isEmpty() || context.getText(key) in values } }
    }
}
