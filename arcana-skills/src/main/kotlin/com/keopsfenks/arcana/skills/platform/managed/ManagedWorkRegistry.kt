package com.keopsfenks.arcana.skills.platform.managed
import com.keopsfenks.arcana.core.skills.SkillRegistration
import com.keopsfenks.arcana.core.skills.platform.SkillActor
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
    inner class ManagedLease internal constructor(val owner: SkillRegistration, val actor: SkillActor, val dependencies: Set<SkillRegistration>) : AutoCloseable {
        val id: UUID = UUID.randomUUID()
        private val alive = AtomicBoolean(true)
        private val cleanup = ConcurrentHashMap.newKeySet<() -> Unit>()
        val active get() = alive.get() && owner.isActive && dependencies.all { it.isActive }
        fun onClose(action: () -> Unit) { cleanup.add(action); if (!alive.get() && cleanup.remove(action)) safely(action) }
        override fun close() {
            val actions = lifecycle.locked {
                if (!alive.compareAndSet(true, false)) emptyList() else {
                    entries.remove(id); actors.computeIfPresent(actor.id()) { _, n -> (n - 1).takeIf { it > 0 } }
                    cleanup.toList().filter { cleanup.remove(it) }
                }
            }
            actions.forEach(::safely)
        }
    }
    fun reserve(owner: SkillRegistration, actor: SkillActor, dependencies: Set<SkillRegistration> = emptySet()): ManagedLease = lifecycle.locked {
        if (stopped || !owner.isActive || !lifecycle.available(owner.owner) || dependencies.any { !it.isActive || !lifecycle.available(it.owner) }) throw PlatformDependencyException("Managed owner or definition unavailable")
        if (entries.size >= engineLimit || (actors[actor.id()] ?: 0) >= actorLimit) throw PlatformBudgetException("Managed instance limit reached ($actorLimit actor / $engineLimit engine)")
        ManagedLease(owner, actor, java.util.Set.copyOf(dependencies)).also { entries[it.id] = it; actors.merge(actor.id(), 1, Int::plus) }
    }
    fun removeActor(id: UUID) = lifecycle.locked { entries.values.filter { it.actor.id() == id } }.forEach { it.close() }
    fun remove(handle: SkillRegistration) = lifecycle.locked { entries.values.filter { it.owner === handle || handle in it.dependencies } }.forEach { it.close() }
    override fun close() { lifecycle.locked { stopped = true; entries.values.toList() }.forEach { it.close() } }
    private fun safely(action: () -> Unit) { try { action() } catch (error: Exception) { report("Could not clean managed contribution: ${error.message}") } }
}
