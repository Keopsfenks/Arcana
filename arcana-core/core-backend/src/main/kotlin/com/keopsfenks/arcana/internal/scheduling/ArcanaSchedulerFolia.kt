package com.keopsfenks.arcana.internal.scheduling

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.scheduling.AsyncTaskContext
import com.keopsfenks.arcana.core.scheduling.EntityTaskContext
import com.keopsfenks.arcana.core.scheduling.Scheduler
import com.keopsfenks.arcana.core.scheduling.TaskContext
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Entity

private const val CHUNK_SHIFT = 4

/**
 * The scheduler for Folia.
 *
 * Every task submitted is registered here, because Folia's region and entity schedulers
 * offer no way to cancel a plugin's tasks in bulk, so [cancelAll] has nothing to delegate
 * to for those.
 *
 * Loaded only on Folia: this class names Folia types, so touching it on Spigot would fail
 * to link.
 */
class ArcanaSchedulerFolia(
    private val plugin: ArcanaPlugin
) : Scheduler {
    private val registry: MutableSet<FoliaArcanaTask> = ConcurrentHashMap.newKeySet()

    private val global = FoliaGlobalTaskContext(plugin, registry)
    private val async = FoliaAsyncTaskContext(plugin, registry)

    override fun global(): TaskContext = global

    override fun at(location: Location): TaskContext = FoliaRegionTaskContext(
        plugin,
        registry,
        requireNotNull(location.world) { "Cannot schedule at a location with no world" },
        location.blockX shr CHUNK_SHIFT,
        location.blockZ shr CHUNK_SHIFT
    )

    override fun at(world: World, chunkX: Int, chunkZ: Int): TaskContext =
        FoliaRegionTaskContext(plugin, registry, world, chunkX, chunkZ)

    override fun on(entity: Entity): EntityTaskContext =
        FoliaEntityTaskContext(plugin, registry, entity)

    override fun async(): AsyncTaskContext = async

    override fun cancelAll() {
        for (task in registry.toList()) {
            task.cancel()
        }

        registry.clear()

        Bukkit.getGlobalRegionScheduler().cancelTasks(plugin)
        Bukkit.getAsyncScheduler().cancelTasks(plugin)
    }
}
