package com.keopsfenks.arcana.skills.platform.holders
import com.keopsfenks.arcana.core.skills.platform.SkillHolder
import com.keopsfenks.arcana.skills.platform.registry.PlatformRegistration
import com.keopsfenks.arcana.core.skills.platform.SkillHolderProvider
import com.keopsfenks.arcana.skills.platform.sources.DefinitionRevision

data class HolderBinding(val provider: PlatformRegistration<SkillHolderProvider>, val holder: SkillHolder, val revisions: List<DefinitionRevision>)
/** Equal values and exact revision/registration identities preserve their existing native leases. */
object HolderDiff {
    data class Change(val removed: Set<HolderBinding>, val added: Set<HolderBinding>)
    fun between(before: Set<HolderBinding>, after: Set<HolderBinding>) = Change(before - after, after - before)
}
