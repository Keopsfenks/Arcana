package com.keopsfenks.arcana.skills.execution

import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import java.util.PriorityQueue
import java.util.concurrent.CompletableFuture

class ControlledSkillScheduler(var immediate: Boolean = true) : SkillScheduler {
    private data class Pending(val at: Long, val sequence: Long, val action: () -> Unit)
    private val queue = PriorityQueue(compareBy<Pending> { it.at }.thenBy { it.sequence })
    private var tick = 0L
    private var sequence = 0L
    var current: Any? = null
        private set

    var foreignDelayTicks = 0L
    val regionPeers = mutableSetOf<LivingEntity>()
    private fun sameOwner(owner: Any) = current === owner || owner is LivingEntity && current is LivingEntity && owner in regionPeers && current in regionPeers
    fun assertOwner(owner: Any) = check(sameOwner(owner)) { "Foreign context access: expected $owner, current $current" }
    override fun owns(entity: LivingEntity) = sameOwner(entity)
    override fun <T> on(entity: LivingEntity, delayTicks: Long, action: () -> T) = submit(entity, if (delayTicks == 0L && !sameOwner(entity)) foreignDelayTicks else delayTicks, action)
    override fun <T> at(location: Location, action: () -> T) = submit(location, foreignDelayTicks, action)
    override fun <T> global(action: () -> T) = submit("global", foreignDelayTicks, action)

    private fun <T> submit(owner: Any, delay: Long, action: () -> T): ScheduledWork<T> {
        val future = CompletableFuture<T>()
        val run = {
            if (!future.isDone) {
                val previous = current
                current = owner
                try { future.complete(action()) } catch (error: Throwable) { future.completeExceptionally(error) }
                finally { current = previous }
            }
            Unit
        }
        if (immediate && delay == 0L) run() else queue.add(Pending(tick + delay, sequence++, run))
        return ScheduledWork(future) { future.cancel(false) }
    }

    fun advance(ticks: Long = 0) {
        tick += ticks
        var calls = 0
        while (queue.peek()?.at?.let { it <= tick } == true) {
            check(++calls <= 5000) { "Unbounded scheduler loop" }
            queue.remove().action()
        }
    }
}
