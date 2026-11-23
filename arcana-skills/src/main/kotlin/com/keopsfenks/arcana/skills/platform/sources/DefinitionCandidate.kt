package com.keopsfenks.arcana.skills.platform.sources

import com.keopsfenks.arcana.core.skills.SkillReloadResult
import com.keopsfenks.arcana.core.skills.platform.SkillKey
import com.keopsfenks.arcana.skills.compile.SkillSnapshot
import java.util.Collections

class DefinitionSnapshot(entries: Map<SkillKey, DefinitionRevision>) {
    val entries: Map<SkillKey, DefinitionRevision> = Collections.unmodifiableMap(LinkedHashMap(entries))
    val legacy = SkillSnapshot(entries.values.mapNotNull { it.legacy })
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
