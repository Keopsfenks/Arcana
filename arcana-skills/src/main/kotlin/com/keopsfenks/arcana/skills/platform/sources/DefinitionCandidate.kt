package com.keopsfenks.arcana.skills.platform.sources

import com.keopsfenks.arcana.core.skills.SkillReloadResult
import com.keopsfenks.arcana.core.skills.platform.SkillKey
import com.keopsfenks.arcana.skills.compile.SkillSnapshot
import java.util.Collections

class DefinitionSnapshot(entries: Map<SkillKey, DefinitionRevision>) {
    val entries: Map<SkillKey, DefinitionRevision> = Collections.unmodifiableMap(LinkedHashMap(entries))
    val legacy = SkillSnapshot(entries.values.mapNotNull { it.legacy }, this)
    val normalTriggers = this.entries.values.filter { it.enabled && it.native?.let { skill -> !skill.holderOnly && skill.interceptors.isEmpty() } == true }
        .flatMap { revision -> revision.native!!.triggers.map { it to revision } }.groupBy({ it.first }, { it.second })
    companion object { val EMPTY = DefinitionSnapshot(emptyMap()) }
}

data class DefinitionCandidate(
    val base: DefinitionSnapshot,
    val snapshot: DefinitionSnapshot?,
    val epoch: Long,
    val sources: List<SourceRegistration>,
    val affected: Set<SkillKey>,
    val fresh: Set<SkillKey>,
    val full: Boolean,
    val report: SkillReloadResult
)
