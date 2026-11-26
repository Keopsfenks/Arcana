package com.keopsfenks.arcana.skills.platform.diagnostics
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.DefaultSkillEngine

class SkillInspector(private val engine: DefaultSkillEngine) {
    fun component(kind: String, key: SkillKey): List<String> {
        fun <T : SkillPlatformComponent> read(type: SkillComponentType<T>): List<String> {
            val entry = engine.platform().registry(type).get(key) ?: return listOf("Unknown $kind component: $key")
            val descriptor = entry.descriptor
            return listOf("$kind $key: ${descriptor.description().take(1024)}") + descriptor.arguments().take(64).map { "${it.name()} (${it.type()}): ${it.description().take(256)}" }
        }
        return when (kind) {
            "mechanic" -> read(SkillComponentType.MECHANIC); "predicate" -> read(SkillComponentType.PREDICATE)
            "targeter" -> read(SkillComponentType.TARGETER); "mutator" -> read(SkillComponentType.MUTATOR)
            "interceptor" -> read(SkillComponentType.INTERCEPTOR); "contribution" -> read(SkillComponentType.CONTRIBUTION)
            "aura_component" -> read(SkillComponentType.AURA_COMPONENT); "trigger" -> read(SkillComponentType.TRIGGER)
            "resource" -> read(SkillComponentType.RESOURCE); "stat" -> read(SkillComponentType.STAT)
            else -> listOf("Unknown component kind: $kind")
        }
    }
    fun validate() = engine.prepareReload().report
    fun inspect(key: SkillKey): List<String> {
        val revision = engine.platform().repository.snapshot().entries[key] ?: return listOf("Unknown skill: $key")
        val info = revision.info()
        val native = revision.native
        return listOf("${info.key()} schema=${info.schema()} enabled=${info.enabled()} available=${info.available()}",
            "source=${info.source()} origin=${info.origin()}", "revision=${revision.revisionId}",
            "dependencies=${revision.dependencies.map { it.id }.sorted().joinToString()}",
            "triggers=${native?.triggers ?: revision.legacy?.triggers.orEmpty()} activation=${if (native?.holderOnly == true) "holder" else "global"}",
            "snapshots=${native?.snapshots?.size ?: 0} parameters=${native?.parameters?.size ?: 0} costs=${native?.costs?.size ?: 0}")
    }
}
