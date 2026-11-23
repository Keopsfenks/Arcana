package com.keopsfenks.arcana.internal.scheduling

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.scheduling.ArcanaTask
import com.keopsfenks.arcana.core.scheduling.RunnableTask

/**
 * A [RunnableTask] that submits itself through the plugin's scheduler.
 *
 * No longer a `BukkitRunnable`: that type's submit methods throw on Folia, and there is
 * nothing left that needs it now that [ArcanaTask] carries cancellation.
 *
 * `BukkitRunnable` refused to schedule the same instance twice, throwing
 * `IllegalStateException`, and synchronised every submit method. Both are reproduced here
 * so that behaviour on Paper and Spigot is unchanged: a task is submittable exactly once,
 * including after it has been cancelled, which is what `BukkitRunnable` did.
 */
@Suppress("DEPRECATION")
abstract class ArcanaRunnableTask(
    protected val plugin: ArcanaPlugin
) : RunnableTask {
    private var scheduled: ArcanaTask? = null

    @Synchronized
    private fun submit(submitter: () -> ArcanaTask): ArcanaTask {
        check(scheduled == null) { "Already scheduled" }

        val task = submitter()
        scheduled = task
        return task
    }

    override fun runTask(): ArcanaTask =
        submit { plugin.scheduler.global().run(this) }

    override fun runTaskAsynchronously(): ArcanaTask =
        submit { plugin.scheduler.async().run(this) }

    override fun runTaskLater(delay: Long): ArcanaTask =
        submit { plugin.scheduler.global().runLater(this, delay) }

    override fun runTaskLaterAsynchronously(delay: Long): ArcanaTask =
        submit { plugin.scheduler.async().runLater(this, delay) }

    override fun runTaskTimer(delay: Long, period: Long): ArcanaTask =
        submit { plugin.scheduler.global().runTimer(this, delay, period) }

    override fun runTaskTimerAsynchronously(delay: Long, period: Long): ArcanaTask =
        submit { plugin.scheduler.async().runTimer(this, delay, period) }

    /**
     * Cancel the submitted task.
     *
     * `BukkitRunnable.cancel` threw `IllegalStateException` when the runnable had never
     * been scheduled. This is a no-op instead: the only reason this method still exists is
     * for jars compiled against arcana 6, and throwing at them from a compatibility shim is
     * worse than doing nothing. Cancelling a task that has been submitted behaves exactly
     * as it did.
     */
    @Synchronized
    override fun cancel() {
        scheduled?.cancel()
    }
}
