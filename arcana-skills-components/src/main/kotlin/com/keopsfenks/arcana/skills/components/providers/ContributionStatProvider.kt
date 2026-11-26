package com.keopsfenks.arcana.skills.components.providers
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.holders.ContributionStore
import java.util.concurrent.CompletableFuture
/** Pure cached contribution sums; no live entity or provider reads. */
class ContributionStatProvider(private val contributions: ContributionStore) : SkillStatProvider {
    override fun descriptor() = SkillDescriptor.of("contributions", "Sum of owner-qualified passive stat contributions")
    override fun supportsLocal() = true
    override fun snapshotLocal(actor: SkillActor, keys: Set<SkillKey>): Map<SkillKey, Double> {
        val values = contributions.snapshot(actor)
        return java.util.Map.copyOf(keys.associateWith { values[it] ?: 0.0 })
    }
    override fun snapshot(actor: SkillActor, keys: Set<SkillKey>) = CompletableFuture.completedFuture(snapshotLocal(actor, keys))
}
