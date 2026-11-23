package com.keopsfenks.arcana.internal.scheduling

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.scheduling.ArcanaTask
import com.keopsfenks.arcana.core.scheduling.TaskContext
import java.util.function.Consumer
import org.bukkit.Bukkit

/**
 * The only [TaskContext] Paper and Spigot need. Region and entity contexts both resolve
 * to this, because there is one thread to run on, so every call is the same
 * `BukkitScheduler` call arcana has always made.
 */
open class BukkitTaskContext(
    private val plugin: ArcanaPlugin
) : TaskContext {
    override fun run(runnable: Runnable): ArcanaTask {
        val task = BukkitArcanaTask(plugin, false, true)
        task.bind(Bukkit.getScheduler().runTask(plugin, runnable))
        return task
    }

    override fun runLater(runnable: Runnable, ticksLater: Long): ArcanaTask {
        val task = BukkitArcanaTask(plugin, false, true)
        task.bind(Bukkit.getScheduler().runTaskLater(plugin, runnable, ticksLater))
        return task
    }

    override fun runTimer(runnable: Runnable, delay: Long, repeat: Long): ArcanaTask {
        val task = BukkitArcanaTask(plugin, true, true)
        task.bind(Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, repeat))
        return task
    }

    override fun runTimer(runnable: Consumer<ArcanaTask>, delay: Long, repeat: Long): ArcanaTask {
        val task = BukkitArcanaTask(plugin, true, true)
        task.bind(
            Bukkit.getScheduler().runTaskTimer(plugin, Runnable { runnable.accept(task) }, delay, repeat)
        )
        return task
    }
}
