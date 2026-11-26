package com.keopsfenks.arcana.skills.platform.values

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import java.util.Collections
import java.util.Locale
import java.util.Optional
import java.util.UUID

object ParameterBindings {
    fun bind(declarations: List<SkillParameter>, supplied: Map<String, SkillValue>): Map<String, SkillValue> {
        val byName = declarations.associateBy { it.name() }
        require(byName.size == declarations.size) { "Duplicate parameter declaration" }
        supplied.keys.firstOrNull { it !in byName }?.let { throw IllegalArgumentException("parameters.$it: unknown parameter") }
        val result = LinkedHashMap<String, SkillValue>()
        for (declaration in declarations) {
            val value = supplied[declaration.name()] ?: declaration.defaultValue().orElse(null)
            require(value != null || !declaration.required()) { "parameters.${declaration.name()}: required parameter" }
            if (value != null) {
                require(value.kind() == declaration.type()) { "parameters.${declaration.name()}: expected ${declaration.type()}" }
                result[declaration.name()] = value
            }
        }
        SkillValue.of(result) // Enforce the combined payload bound, not only one parameter at a time.
        return Collections.unmodifiableMap(result)
    }

    fun declarations(arguments: SkillArguments, namespace: String): List<SkillParameter> {
        if (!arguments.has("parameters")) return emptyList()
        val section = arguments.section("parameters")
        return java.util.List.copyOf(section.values.map { (name, raw) ->
            val args = section.child(name, raw)
            args.values.keys.firstOrNull { it !in setOf("type", "default", "required") }?.let { throw args.error(it, "unknown parameter option") }
            val type = kind(args.string("type"), "${args.path}.type")
            val default = if (args.has("default")) Optional.of(literal(type, args.raw("default"), namespace, "${args.path}.default")) else Optional.empty()
            try { SkillParameter(name, type, args.bool("required", !args.has("default")), default) }
            catch (error: IllegalArgumentException) { throw args.error("", error.message ?: "invalid parameter") }
        })
    }

    fun kind(name: String, path: String): SkillValue.Kind = when (name.lowercase(Locale.ROOT)) {
        "number" -> SkillValue.Kind.NUMBER
        "text", "string" -> SkillValue.Kind.TEXT
        "boolean" -> SkillValue.Kind.BOOLEAN
        "list" -> SkillValue.Kind.LIST
        "map" -> SkillValue.Kind.MAP
        "key" -> SkillValue.Kind.KEY
        "location" -> SkillValue.Kind.LOCATION
        "vector" -> SkillValue.Kind.VECTOR
        else -> throw IllegalArgumentException("$path: unknown value type $name")
    }

    fun literal(type: SkillValue.Kind, raw: Any?, namespace: String, path: String): SkillValue {
        val value = try {
            when {
                raw is SkillValue -> raw
                type == SkillValue.Kind.KEY && raw is String -> SkillValue.of(SkillKey.parse(raw, namespace))
                type == SkillValue.Kind.VECTOR && raw is Map<*, *> -> SkillValue.of(SkillVector(
                    (raw["x"] as Number).toDouble(), (raw["y"] as Number).toDouble(), (raw["z"] as Number).toDouble()))
                type == SkillValue.Kind.LOCATION && raw is Map<*, *> -> SkillValue.of(SkillLocation(
                    UUID.fromString(raw["world"] as String), (raw["x"] as Number).toDouble(), (raw["y"] as Number).toDouble(), (raw["z"] as Number).toDouble(),
                    (raw["yaw"] as? Number)?.toFloat() ?: 0f, (raw["pitch"] as? Number)?.toFloat() ?: 0f))
                else -> SkillValue.of(raw)
            }
        } catch (error: Exception) { throw IllegalArgumentException("$path: invalid $type value: ${error.message}", error) }
        require(value.kind() == type) { "$path: expected $type" }
        return value
    }
}
