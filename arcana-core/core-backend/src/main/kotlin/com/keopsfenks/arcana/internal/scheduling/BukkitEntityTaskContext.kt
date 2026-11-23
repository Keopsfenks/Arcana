package com.keopsfenks.arcana.internal.scheduling

import com.keopsfenks.arcana.core.scheduling.ArcanaTask
import com.keopsfenks.arcana.core.scheduling.EntityTaskContext
import com.keopsfenks.arcana.core.scheduling.TaskContext
import java.util.function.Consumer

/**
 * Entity affinity means nothing on Paper and Spigot, so this delegates everything to the
 * sync context and discards the retirement action.
 */
class BukkitEntityTaskContext(
    private val delegate: TaskContext
) : EntityTaskContext {
    override fun onRetired(onRetired: Runnable): EntityTaskContext = this

    override fun run(runnable: Runnable): ArcanaTask = delegate.run(runnable)

    override fun runLater(runnable: Runnable, ticksLater: Long): ArcanaTask =
        delegate.runLater(runnable, ticksLater)

    override fun runTimer(runnable: Runnable, delay: Long, repeat: Long): ArcanaTask =
        delegate.runTimer(runnable, delay, repeat)

    override fun runTimer(runnable: Consumer<ArcanaTask>, delay: Long, repeat: Long): ArcanaTask =
        delegate.runTimer(runnable, delay, repeat)
}
