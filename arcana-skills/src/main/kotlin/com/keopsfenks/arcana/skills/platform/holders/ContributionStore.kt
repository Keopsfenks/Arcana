package com.keopsfenks.arcana.skills.platform.holders
import com.keopsfenks.arcana.core.skills.SkillRegistration
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionContext
import com.keopsfenks.arcana.skills.platform.lifecycle.SkillLifecycleGate
import com.keopsfenks.arcana.skills.platform.managed.ManagedWorkRegistry
import java.util.UUID
import java.util.concurrent.*

class ContributionStore(private val registry: ManagedWorkRegistry, private val lifecycle: SkillLifecycleGate, private val report: (String) -> Unit) {
    private data class Entry(val actor: UUID, val stats: Map<SkillKey, Double>)
    private val entries = ConcurrentHashMap<UUID, Entry>()
    fun apply(owner: SkillRegistration, context: PlatformExecutionContext, contribution: CompiledSkillContribution): CompletableFuture<ManagedWorkRegistry.ManagedLease> {
        val lease = registry.reserve(owner, context.invocation().actor(), context.state.root.dependencies)
        val result = CompletableFuture<ManagedWorkRegistry.ManagedLease>()
        lease.onClose { entries.remove(lease.id); result.completeExceptionally(CancellationException("Contribution retired")) }
        try {
            val identity = SkillKey(owner.id.substringBefore(':'), "contribution_${lease.id.toString().replace("-", "_")}")
            context.state.checkActive(); check(lease.active) { "Contribution retired before application" }
            contribution.apply(context, identity).whenComplete { native, error ->
                if (error != null) { result.completeExceptionally(error); lease.close() }
                else try {
                    requireNotNull(native)
                    // Snapshot extension metadata outside the lifecycle gate.
                    val stats = java.util.Map.copyOf(native.stats())
                    require(stats.values.all { it.isFinite() }) { "Nonfinite passive stat contribution" }
                    lease.onClose { try { native.close() } catch (failure: Exception) { report("Could not remove native contribution: ${failure.message}") } }
                    val published = lifecycle.locked {
                        if (!lease.active || !lifecycle.available(owner.owner)) false
                        else { entries[lease.id] = Entry(lease.actor.id(), stats); true }
                    }
                    if (published) result.complete(lease) else lease.close()
                } catch (failure: Throwable) {
                    try { native?.close() } finally { result.completeExceptionally(failure); lease.close() }
                }
            }
        } catch (error: Throwable) { result.completeExceptionally(error); lease.close() }
        return result
    }
    fun update(lease: ManagedWorkRegistry.ManagedLease, values: Map<SkillKey, Double>): Boolean {
        val copy = java.util.Map.copyOf(values)
        require(copy.size <= 256 && copy.values.all { it.isFinite() }) { "Expected at most 256 finite stat values" }
        return lifecycle.locked {
            if (!lease.active || !entries.containsKey(lease.id)) false
            else { entries[lease.id] = Entry(lease.actor.id(), copy); true }
        }
    }
    fun snapshot(actor: SkillActor): Map<SkillKey, Double> {
        val values = LinkedHashMap<SkillKey, Double>()
        lifecycle.locked { entries.values.filter { it.actor == actor.id() }.forEach { entry -> entry.stats.forEach { (key, amount) -> values.merge(key, amount, Double::plus) } } }
        require(values.values.all { it.isFinite() }) { "Passive stat sum is nonfinite" }
        return java.util.Map.copyOf(values)
    }
}
