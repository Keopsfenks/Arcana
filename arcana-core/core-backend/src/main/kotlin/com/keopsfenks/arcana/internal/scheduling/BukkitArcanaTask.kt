package com.keopsfenks.arcana.internal.scheduling

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.scheduling.ArcanaTask
import org.bukkit.scheduler.BukkitTask

/**
 * An [ArcanaTask] backed by a [BukkitTask].
 *
 * The handle is created before the task is submitted, so that a repeating task can be
 * handed its own handle, and is bound afterwards. Cancelling an unbound handle is
 * remembered and applied on [bind].
 */
class BukkitArcanaTask(
    private val plugin: ArcanaPlugin,
    private val repeating: Boolean,
    private val sync: Boolean
) : ArcanaTask {
    @Volatile
    private var handle: BukkitTask? = null

    @Volatile
    private var cancelRequested = false

    fun bind(task: BukkitTask?) {
        if (task == null) {
            cancelRequested = true
            return
        }

        handle = task

        if (cancelRequested) {
            task.cancel()
        }
    }

    override fun cancel() {
        cancelRequested = true
        handle?.cancel()
    }

    override fun isCancelled(): Boolean = cancelRequested || (handle?.isCancelled ?: false)

    override fun isRepeating(): Boolean = repeating

    override fun getPlugin(): ArcanaPlugin = plugin

    /**
     * The real Bukkit ID once bound. -1 before then, and after a task that was cancelled
     * before it could be submitted, because there is no ID to report.
     */
    @Deprecated("Hold the ArcanaTask and cancel that instead.")
    @Suppress("DEPRECATION")
    override fun getTaskId(): Int = handle?.taskId ?: -1

    @Deprecated("Ask the context the task was submitted through instead.")
    @Suppress("DEPRECATION")
    override fun isSync(): Boolean = sync

    @Deprecated("An ArcanaTask is already a BukkitTask.")
    @Suppress("DEPRECATION")
    override fun asBukkitTask(): BukkitTask = this
}
