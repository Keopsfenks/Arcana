package com.keopsfenks.arcana.skills.platform.managed
import com.keopsfenks.arcana.core.skills.SkillRegistration
import com.keopsfenks.arcana.core.skills.platform.SkillActor
import com.keopsfenks.arcana.core.skills.platform.SkillRemovalReason
import com.keopsfenks.arcana.skills.platform.execution.PlatformBudgetException
import com.keopsfenks.arcana.skills.platform.execution.PlatformDependencyException
import com.keopsfenks.arcana.skills.platform.lifecycle.SkillLifecycleGate
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/** One bounded index for passives and subsequently adopted aura/projectile work. */
class ManagedWorkRegistry(private val lifecycle: SkillLifecycleGate, private val report: (String) -> Unit, val actorLimit: Int = 64, val engineLimit: Int = 4096) : AutoCloseable {
    init { require(actorLimit in 1..64 && engineLimit in 1..4096) }
    private val entries = LinkedHashMap<UUID, ManagedLease>()
    private val actors = HashMap<UUID, Int>()
    private var stopped = false
    val size get() = lifecycle.locked { entries.size }
    inner class ManagedLease internal constructor(val owner: SkillRegistration, val actor: SkillActor, val dependencies: Set<SkillRegistration>, val relatedActors: Set<UUID>) : AutoCloseable {
        val id: UUID = UUID.randomUUID()
        private val alive = AtomicBoolean(true)
        private val removal = java.util.concurrent.atomic.AtomicReference<SkillRemovalReason?>()
        val reason get() = removal.get()
        private val cleanup = ConcurrentHashMap.newKeySet<() -> Unit>()
        val active get() = alive.get() && owner.isActive && lifecycle.available(owner.owner) && dependencies.all { it.isActive && lifecycle.available(it.owner) }
        fun onClose(action: () -> Unit) { cleanup.add(action); if (!alive.get() && cleanup.remove(action)) safely(action) }
        internal fun markReason(reason: SkillRemovalReason) { removal.compareAndSet(null, reason) }
        override fun close() = remove(SkillRemovalReason.CANCELLED)
        fun remove(reason: SkillRemovalReason) {
            val actions = lifecycle.locked {
                if (!alive.compareAndSet(true, false)) emptyList() else {
                    markReason(reason)
                    entries.remove(id); relatedActors.forEach { actorId -> actors.computeIfPresent(actorId) { _, n -> (n - 1).takeIf { it > 0 } } }
                    cleanup.toList().filter { cleanup.remove(it) }
                }
            }
            actions.forEach(::safely)
        }
    }
    fun reserve(owner: SkillRegistration, actor: SkillActor, dependencies: Set<SkillRegistration> = emptySet(), relatedActors: Set<UUID> = emptySet()): ManagedLease = lifecycle.locked {
        if (stopped || !owner.isActive || !lifecycle.available(owner.owner) || dependencies.any { !it.isActive || !lifecycle.available(it.owner) }) throw PlatformDependencyException("Managed owner or definition unavailable")
        val subjects = java.util.Set.copyOf(relatedActors + actor.id())
        if (entries.size >= engineLimit || subjects.any { (actors[it] ?: 0) >= actorLimit }) throw PlatformBudgetException("Managed instance limit reached ($actorLimit actor / $engineLimit engine)")
        ManagedLease(owner, actor, java.util.Set.copyOf(dependencies), subjects).also { entries[it.id] = it; subjects.forEach { id -> actors.merge(id, 1, Int::plus) } }
    }
    private fun retire(leases: List<ManagedLease>, reason: SkillRemovalReason) {
        lifecycle.locked { leases.forEach { it.markReason(reason) } }
        leases.forEach { it.remove(reason) }
    }
    fun removeActor(id: UUID, reason: SkillRemovalReason = SkillRemovalReason.CANCELLED) = retire(lifecycle.locked { entries.values.filter { id in it.relatedActors } }, reason)
    fun remove(handle: SkillRegistration) {
        val reason = lifecycle.locked {
            if (!lifecycle.available(handle.owner)) SkillRemovalReason.OWNER_UNLOADED
            else if (handle is com.keopsfenks.arcana.skills.platform.sources.DefinitionRevision || handle is com.keopsfenks.arcana.skills.platform.sources.SourceRegistration) SkillRemovalReason.DEFINITION_RELOADED
            else SkillRemovalReason.OWNER_UNLOADED
        }
        retire(lifecycle.locked { entries.values.filter { it.owner === handle || handle in it.dependencies } }, reason)
    }
    override fun close() { retire(lifecycle.locked { stopped = true; entries.values.toList() }, SkillRemovalReason.ENGINE_STOPPED) }
    private fun safely(action: () -> Unit) { try { action() } catch (error: Exception) { report("Could not clean managed contribution: ${error.message}") } }
}
