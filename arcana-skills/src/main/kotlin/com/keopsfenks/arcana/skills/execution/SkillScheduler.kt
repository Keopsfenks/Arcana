package com.keopsfenks.arcana.skills.execution

import com.keopsfenks.arcana.core.Arcana
import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.Prerequisite
import com.keopsfenks.arcana.core.scheduling.ArcanaTask
import com.keopsfenks.arcana.core.scheduling.TaskContext
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference

class ScheduledWork<T>(val future: CompletableFuture<T>, private val cancelAction: () -> Unit) {
    private val cancelled = java.util.concurrent.atomic.AtomicBoolean()
    fun cancel() {
        if (cancelled.compareAndSet(false, true)) {
            future.cancel(false)
            try { cancelAction() } catch (_: Exception) { /* The cancelled future still prevents the callback. */ }
        }
    }
}

interface SkillScheduler {
    fun <T> on(entity: LivingEntity, delayTicks: Long = 0, action: () -> T): ScheduledWork<T>
    fun <T> at(location: Location, action: () -> T): ScheduledWork<T>
    fun <T> global(action: () -> T): ScheduledWork<T>
}

class ArcanaSkillScheduler(private val plugin: ArcanaPlugin) : SkillScheduler {
    override fun <T> on(entity: LivingEntity, delayTicks: Long, action: () -> T): ScheduledWork<T> {
        val future = CompletableFuture<T>()
        val owned = if (Prerequisite.HAS_FOLIA.isMet) Arcana.get().isOwnedByCurrentRegion(entity) else Bukkit.isPrimaryThread()
        val context = plugin.scheduler.on(entity).onRetired { future.cancel(false) }
        return submit(context, owned, delayTicks, future, action)
    }

    override fun <T> at(location: Location, action: () -> T): ScheduledWork<T> {
        val owned = if (Prerequisite.HAS_FOLIA.isMet) Arcana.get().isOwnedByCurrentRegion(location) else Bukkit.isPrimaryThread()
        return submit(plugin.scheduler.at(location), owned, 0, CompletableFuture(), action)
    }

    override fun <T> global(action: () -> T): ScheduledWork<T> {
        val owned = if (Prerequisite.HAS_FOLIA.isMet) Bukkit.isGlobalTickThread() else Bukkit.isPrimaryThread()
        return submit(plugin.scheduler.global(), owned, 0, CompletableFuture(), action)
    }

    private fun <T> submit(context: TaskContext, owned: Boolean, delay: Long, future: CompletableFuture<T>, action: () -> T): ScheduledWork<T> {
        val handle = AtomicReference<ArcanaTask?>()
        val runnable = Runnable {
            if (!future.isDone) {
                try { future.complete(action()) } catch (error: Throwable) { future.completeExceptionally(error) }
            }
        }
        try {
            if (owned && delay == 0L) runnable.run()
            else {
                val task = if (delay > 0) context.runLater(runnable, delay) else context.run(runnable)
                handle.set(task)
                if (future.isCancelled) task.cancel()
            }
        } catch (error: Throwable) { future.completeExceptionally(error) }
        return ScheduledWork(future) { handle.get()?.cancel() }
    }
}
