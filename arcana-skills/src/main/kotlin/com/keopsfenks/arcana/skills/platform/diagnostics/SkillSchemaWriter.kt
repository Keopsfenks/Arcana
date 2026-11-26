package com.keopsfenks.arcana.skills.platform.diagnostics
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.compile.NodeOptions
import com.keopsfenks.arcana.skills.platform.compile.PlatformSkillCompiler

data class SkillCatalogEntry(val kind: String, val key: SkillKey, val descriptor: SkillDescriptor)

/** Authoring schema; compilation remains authoritative for expressions, references and semantic rules. */
object SkillSchemaWriter {
    fun quote(value: String): String = "\"" + buildString { value.forEach { c -> when (c) { '"' -> append("\\\""); '\\' -> append("\\\\"); '\n' -> append("\\n"); '\r' -> append("\\r"); '\t' -> append("\\t"); else -> if (c.code < 32) append("\\u%04x".format(c.code)) else append(c) } } } + "\""
    private fun json(value: Any?): String = when (value) {
        null -> "null"; is String -> quote(value); is Boolean, is Number -> value.toString()
        is Map<*, *> -> value.entries.joinToString(",", "{", "}") { quote(it.key.toString()) + ":" + json(it.value) }
        is Iterable<*> -> value.joinToString(",", "[", "]") { json(it) }
        else -> error("Unsupported schema value")
    }
    private fun ref(name: String) = mapOf("\u0024ref" to "#/\u0024defs/" + name.replace("~", "~0").replace("/", "~1"))
    private fun array(items: Any) = mapOf("type" to "array", "maxItems" to 1024, "items" to items)
    private fun field(name: String, type: String = "value"): Map<String, Any> = when (name) {
        "targeter", "select" -> mapOf("anyOf" to listOf(mapOf("type" to "string"), ref("targeter")))
        "mutators" -> array(ref("mutator"))
        "conditions", "filters", "hit-conditions" -> array(ref("predicate"))
        "condition" -> ref("predicate")
        "interceptors" -> array(ref("interceptor"))
        else -> when (type) { "boolean", "string", "array", "object" -> mapOf("type" to type); "list" -> mapOf("type" to "array"); "map" -> mapOf("type" to "object"); else -> emptyMap() }
    }
    fun write(entries: List<SkillCatalogEntry>): String {
        val definitions = linkedMapOf<String, Any>()
        definitions["external"] = mapOf("type" to "object", "properties" to mapOf("type" to mapOf("type" to "string", "pattern" to "^(?!arcana:)[a-z0-9_]+:[a-z0-9_]+$")), "required" to listOf("type"))
        val ordered = entries.sortedWith(compareBy<SkillCatalogEntry> { it.kind }.thenBy { it.key.toString() })
        for (entry in ordered) {
            val properties = linkedMapOf<String, Any>("type" to mapOf("enum" to listOf(entry.key.toString(), entry.key.value())))
            for (arg in entry.descriptor.arguments()) properties[arg.name()] = field(arg.name(), arg.type()) + mapOf("description" to arg.description())
            if (entry.kind == "mechanic") NodeOptions.OPTIONS.filter { it !in properties }.forEach { properties[it] = field(it) }
            definitions["${entry.kind}/${entry.key}"] = mapOf("type" to "object", "description" to entry.descriptor.description(), "properties" to properties,
                "required" to listOf("type") + entry.descriptor.arguments().filter { it.required() }.map { it.name() }, "additionalProperties" to false)
        }
        ordered.groupBy { it.kind }.forEach { (kind, group) -> definitions[kind] = mapOf("anyOf" to (group.map { ref("${it.kind}/${it.key}") } + ref("external"))) }
        val properties = linkedMapOf<String, Any>()
        for (name in PlatformSkillCompiler.ROOT_OPTIONS) properties[name] = when (name) {
            "schema" -> mapOf("const" to 2); "id", "permission", "cooldown-group" -> mapOf("type" to "string")
            "enabled" -> mapOf("type" to "boolean"); "activation" -> mapOf("enum" to listOf("global", "holder"))
            "triggers" -> array(mapOf("type" to "string")); "effects" -> array(ref("mechanic")); "passives" -> array(ref("contribution"))
            "conditions" -> array(ref("predicate")); "interceptors" -> array(ref("interceptor")); "costs" -> mapOf("type" to "array")
            "parameters", "bindings", "charges", "templates" -> mapOf("type" to "object"); else -> emptyMap<String, Any>()
        }
        return json(linkedMapOf("\u0024schema" to "https://json-schema.org/draft/2020-12/schema", "title" to "Arcana native skill schema 2", "description" to "Authoring schema generated from factory option metadata. Run skills validate for semantic checks and external components.",
            "type" to "object", "properties" to properties, "required" to listOf("schema"), "additionalProperties" to false, "\u0024defs" to definitions)) + "\n"
    }
}
