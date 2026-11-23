package com.keopsfenks.arcana.internal.spigot.drops

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.internal.drops.ArcanaDropQueue
import com.keopsfenks.arcana.internal.drops.ArcanaFastCollatedDropQueue
import com.keopsfenks.arcana.internal.drops.ArcanaFastCollatedDropQueue.CollatedDrops
import org.bukkit.entity.Player

/**
 * Take every pending entry and hand it to [dispatch].
 *
 * Entries are removed one at a time, as they are taken, and never in bulk: another thread
 * can add a drop while this is running, and clearing the map afterwards would discard it.
 * The remove is also what claims an entry, so no entry is dispatched twice.
 */
internal fun drainCollatedDrops(
    map: MutableMap<Player, CollatedDrops>,
    dispatch: (Player, CollatedDrops) -> Unit
) {
    for (player in map.keys.toList()) {
        val drops = map.remove(player) ?: continue
        dispatch(player, drops)
    }
}

/**
 * Pushes collated drops once a tick.
 *
 * The drain runs globally, but each queue is pushed on the region owning the drop
 * location, because pushing spawns items and experience orbs into the world.
 */
class CollatedRunnable(plugin: ArcanaPlugin) {
    init {
        plugin.scheduler.global().runTimer(0, 1) {
            drainCollatedDrops(ArcanaFastCollatedDropQueue.COLLATED_MAP) { player, drops ->
                val location = drops.location

                plugin.scheduler.at(location).run {
                    val queue = ArcanaDropQueue(player)
                        .setLocation(location)
                        .addItems(drops.drops)
                        .addXP(drops.xp)

                    if (drops.telekinetic) {
                        queue.forceTelekinesis()
                    }

                    queue.push()
                }
            }
        }
    }
}
