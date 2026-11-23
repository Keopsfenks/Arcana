package com.keopsfenks.arcana.internal.scheduling

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.scheduling.AsyncTaskContext
import com.keopsfenks.arcana.core.scheduling.EntityTaskContext
import com.keopsfenks.arcana.core.scheduling.Scheduler
import com.keopsfenks.arcana.core.scheduling.TaskContext
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Entity

/**
 * The scheduler for Paper and Spigot.
 *
 * There is one thread, so every context resolves to the same [BukkitTaskContext] and the
 * contexts are allocated once. Behaviour is identical to calling [Bukkit.getScheduler]
 * directly, which is the point: this is the path 99% of servers take.
 */
class ArcanaSchedulerBukkit(
    private val plugin: ArcanaPlugin
) : Scheduler {
    private val sync = BukkitTaskContext(plugin)
    private val entity = BukkitEntityTaskContext(sync)
    private val async = BukkitAsyncTaskContext(plugin)

    override fun global(): TaskContext = sync

    override fun at(location: Location): TaskContext = sync

    override fun at(world: World, chunkX: Int, chunkZ: Int): TaskContext = sync

    override fun on(entity: Entity): EntityTaskContext = this.entity

    override fun async(): AsyncTaskContext = async

    override fun cancelAll() {
        Bukkit.getScheduler().cancelTasks(plugin)
    }
}
