package com.keopsfenks.arcana.internal.scheduling

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.scheduling.AsyncTaskContext
import com.keopsfenks.arcana.core.scheduling.ArcanaTask
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import org.bukkit.Bukkit

private const val MILLIS_PER_TICK = 50L

/**
 * Off-thread tasks on Folia.
 *
 * Folia's async scheduler measures time, not ticks, so tick-based delays are converted.
 * That makes them wall-clock rather than tick-driven, which is a real difference from
 * Paper on a lagging server, and the reason the [TimeUnit] overloads exist.
 *
 * Loaded only on Folia.
 */
class FoliaAsyncTaskContext(
    private val plugin: ArcanaPlugin,
    private val registry: MutableSet<FoliaArcanaTask>
) : AsyncTaskContext {
    private fun submit(repeating: Boolean, submitter: (FoliaArcanaTask) -> Unit): ArcanaTask {
        val task = FoliaArcanaTask(plugin, repeating, registry, false)
        registry.add(task)
        submitter(task)
        return task
    }

    override fun run(runnable: Runnable): ArcanaTask = submit(false) { task ->
        task.bind(Bukkit.getAsyncScheduler().runNow(plugin, task.wrap(runnable)))
    }

    override fun runLater(runnable: Runnable, ticksLater: Long): ArcanaTask {
        if (ticksLater <= 0) {
            return run(runnable)
        }

        return runLater(runnable, ticksLater * MILLIS_PER_TICK, TimeUnit.MILLISECONDS)
    }

    override fun runLater(runnable: Runnable, delay: Long, unit: TimeUnit): ArcanaTask =
        submit(false) { task ->
            task.bind(
                Bukkit.getAsyncScheduler().runDelayed(plugin, task.wrap(runnable), delay, unit)
            )
        }

    override fun runTimer(runnable: Runnable, delay: Long, repeat: Long): ArcanaTask =
        runTimer(
            runnable,
            delay.coerceAtLeast(0) * MILLIS_PER_TICK,
            repeat.coerceAtLeast(1) * MILLIS_PER_TICK,
            TimeUnit.MILLISECONDS
        )

    override fun runTimer(
        runnable: Runnable,
        delay: Long,
        period: Long,
        unit: TimeUnit
    ): ArcanaTask = submit(true) { task ->
        task.bind(
            Bukkit.getAsyncScheduler()
                .runAtFixedRate(plugin, task.wrap(runnable), delay, period, unit)
        )
    }

    override fun runTimer(runnable: Consumer<ArcanaTask>, delay: Long, repeat: Long): ArcanaTask =
        submit(true) { task ->
            task.bind(
                Bukkit.getAsyncScheduler().runAtFixedRate(
                    plugin,
                    task.wrapSelf(runnable),
                    delay.coerceAtLeast(0) * MILLIS_PER_TICK,
                    repeat.coerceAtLeast(1) * MILLIS_PER_TICK,
                    TimeUnit.MILLISECONDS
                )
            )
        }
}
