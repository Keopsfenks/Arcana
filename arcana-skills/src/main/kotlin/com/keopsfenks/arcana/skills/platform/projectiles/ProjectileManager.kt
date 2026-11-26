package com.keopsfenks.arcana.skills.platform.projectiles
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.DefaultSkillEngine
import com.keopsfenks.arcana.skills.platform.DefaultSkillPlatform
import com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionContext
import com.keopsfenks.arcana.skills.platform.managed.ManagedExecutionState
import com.keopsfenks.arcana.skills.platform.targets.RegionQuery
import org.bukkit.Bukkit
import org.bukkit.World
import java.util.*
import java.util.concurrent.*

class ProjectileManager private constructor(
    private val engine: DefaultSkillEngine, private val platform: DefaultSkillPlatform,
    private val queryFactory: (ManagedExecutionState) -> SkillTargetQuery, private val worlds: (UUID) -> World?,
    private val entries: ConcurrentHashMap<UUID, ProjectileInstance>, private val stopped: java.util.concurrent.atomic.AtomicBoolean,
    private val bound: PlatformExecutionContext?
) : AutoCloseable {
    constructor(engine: DefaultSkillEngine, platform: DefaultSkillPlatform,
        queryFactory: (ManagedExecutionState) -> SkillTargetQuery = { RegionQuery(engine.scheduler, state = it.state) },
        worlds: (UUID) -> World? = { Bukkit.getWorld(it) }) : this(engine, platform, queryFactory, worlds, ConcurrentHashMap(), java.util.concurrent.atomic.AtomicBoolean(), null)
    fun forContext(context: PlatformExecutionContext) = ProjectileManager(engine, platform, queryFactory, worlds, entries, stopped, context)
    val size get() = entries.size
    fun launch(invocation: SkillInvocation, options: ProjectileOptions, hooks: ProjectileHooks): CompletableFuture<SkillManagedHandle> {
        val context = bound ?: return CompletableFuture.failedFuture(IllegalStateException("Use the invocation-bound ProjectileManager service"))
        return try {
            check(!stopped.get()) { "Projectile manager stopped" }
            val origin = invocation.location().orElseGet { invocation.origin().orElseThrow { IllegalArgumentException("Projectile requires a captured origin") } }
            val managed = ManagedExecutionState.adopt(context, invocation.target().orElse(invocation.actor()), hooks.dependencies, invocation)
            try {
                val instance = ProjectileInstance(managed, invocation, origin, options, hooks, queryFactory(managed), worlds) { entries.remove(it.id(), it) }
                entries[instance.id()] = instance
                if (stopped.get()) instance.remove(SkillRemovalReason.ENGINE_STOPPED)
                if (!instance.active()) entries.remove(instance.id(), instance)
                instance.start(); instance.started
            } catch (error: Throwable) { managed.close(); throw error }
        } catch (error: Throwable) { CompletableFuture.failedFuture(error) }
    }
    override fun close() {
        if (bound == null) stopped.set(true)
        entries.values.toList().filter { bound == null || it.managed.originId == bound.state.root.id }.forEach { it.remove(if (bound == null) SkillRemovalReason.ENGINE_STOPPED else SkillRemovalReason.CANCELLED) }
    }
}
