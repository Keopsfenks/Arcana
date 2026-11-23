package com.keopsfenks.arcana.skills.platform.values

import com.google.gson.*
import com.keopsfenks.arcana.core.skills.platform.*
import java.util.UUID

/** Only closed skill value types are decoded; no reflective Java-object deserialization. */
internal object VariableValueCodec {
    fun encode(record: PersistedVariable): String = JsonObject().apply {
        addProperty("writer", record.writer)
        addProperty("version", record.version)
        record.expiresAtMillis?.let { addProperty("expires", it) }
        add("value", record.value?.let(::encodeValue) ?: JsonNull.INSTANCE)
    }.toString()

    fun decode(text: String): PersistedVariable? {
        if (text.isBlank()) return null
        require(text.length <= 2 * 1024 * 1024) { "Persistent variable exceeds encoded size limit" }
        var depth = 0
        var quoted = false
        var escaped = false
        for (character in text) {
            if (quoted) {
                if (escaped) escaped = false
                else if (character == '\\') escaped = true
                else if (character == '"') quoted = false
            } else when (character) {
                '"' -> quoted = true
                '{', '[' -> { depth++; require(depth <= 2 * SkillValue.MAX_DEPTH + 8) { "Persistent variable nesting is excessive" } }
                '}', ']' -> { depth--; require(depth >= 0) { "Invalid persistent variable" } }
            }
        }
        require(!quoted && depth == 0) { "Incomplete persistent variable" }
        val root = JsonParser.parseString(text).asJsonObject
        val version = root.get("version").asString.toLong()
        require(version >= 0) { "Invalid persistent variable version" }
        val value = root.get("value")
        return PersistedVariable(if (value == null || value.isJsonNull) null else decodeValue(value),
            root.get("expires")?.asString?.toLong(), root.get("writer").asString, version)
    }

    private fun encodeValue(value: SkillValue): JsonObject = JsonObject().apply {
        addProperty("kind", value.kind().name)
        add("data", when (value.kind()) {
            SkillValue.Kind.NUMBER -> JsonPrimitive(value.number())
            SkillValue.Kind.TEXT -> JsonPrimitive(value.text())
            SkillValue.Kind.BOOLEAN -> JsonPrimitive(value.bool())
            SkillValue.Kind.KEY -> JsonPrimitive(value.key().toString())
            SkillValue.Kind.VECTOR, SkillValue.Kind.LOCATION -> Gson().toJsonTree(value.plain())
            SkillValue.Kind.LIST -> JsonArray().also { array -> value.list().forEach { array.add(encodeValue(it)) } }
            SkillValue.Kind.MAP -> JsonObject().also { map -> value.map().forEach { (key, entry) -> map.add(key, encodeValue(entry)) } }
        })
    }

    private fun decodeValue(element: JsonElement): SkillValue {
        val wrapper = element.asJsonObject
        val data = wrapper.get("data")
        val value: Any = when (SkillValue.Kind.valueOf(wrapper.get("kind").asString)) {
            SkillValue.Kind.NUMBER -> data.asDouble
            SkillValue.Kind.TEXT -> data.asString
            SkillValue.Kind.BOOLEAN -> data.asBoolean
            SkillValue.Kind.KEY -> SkillKey.parse(data.asString, "arcana")
            SkillValue.Kind.VECTOR -> data.asJsonObject.let { SkillVector(it.get("x").asDouble, it.get("y").asDouble, it.get("z").asDouble) }
            SkillValue.Kind.LOCATION -> data.asJsonObject.let { SkillLocation(UUID.fromString(it.get("world").asString),
                it.get("x").asDouble, it.get("y").asDouble, it.get("z").asDouble, it.get("yaw").asFloat, it.get("pitch").asFloat) }
            SkillValue.Kind.LIST -> data.asJsonArray.map { decodeValue(it) }
            SkillValue.Kind.MAP -> data.asJsonObject.entrySet().associate { it.key to decodeValue(it.value) }
        }
        return SkillValue.of(value)
    }
}
