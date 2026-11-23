package com.keopsfenks.arcana.internal.scheduling

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.scheduling.AsyncTaskContext
import com.keopsfenks.arcana.core.scheduling.ArcanaTask
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import org.bukkit.Bukkit

private const val MILLIS_PER_TICK = 50L

/**
 * Off-thread tasks on Paper and Spigot. Time units are converted to ticks, because the
 * Bukkit scheduler only counts ticks.
 */
class BukkitAsyncTaskContext(
    private val plugin: ArcanaPlugin
) : AsyncTaskContext {
    override fun run(runnable: Runnable): ArcanaTask {
        val task = BukkitArcanaTask(plugin, false, false)
        task.bind(Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable))
        return task
    }

    override fun runLater(runnable: Runnable, ticksLater: Long): ArcanaTask {
        val task = BukkitArcanaTask(plugin, false, false)
        task.bind(Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, ticksLater))
        return task
    }

    override fun runLater(runnable: Runnable, delay: Long, unit: TimeUnit): ArcanaTask =
        runLater(runnable, unit.toMillis(delay) / MILLIS_PER_TICK)

    override fun runTimer(runnable: Runnable, delay: Long, repeat: Long): ArcanaTask {
        val task = BukkitArcanaTask(plugin, true, false)
        task.bind(
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delay, repeat)
        )
        return task
    }

    override fun runTimer(runnable: Runnable, delay: Long, period: Long, unit: TimeUnit): ArcanaTask =
        runTimer(
            runnable,
            unit.toMillis(delay) / MILLIS_PER_TICK,
            unit.toMillis(period) / MILLIS_PER_TICK
        )

    override fun runTimer(runnable: Consumer<ArcanaTask>, delay: Long, repeat: Long): ArcanaTask {
        val task = BukkitArcanaTask(plugin, true, false)
        task.bind(
            Bukkit.getScheduler()
                .runTaskTimerAsynchronously(plugin, Runnable { runnable.accept(task) }, delay, repeat)
        )
        return task
    }
}
