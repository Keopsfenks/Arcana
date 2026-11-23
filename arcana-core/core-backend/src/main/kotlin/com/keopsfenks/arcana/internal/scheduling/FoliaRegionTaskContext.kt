package com.keopsfenks.arcana.internal.scheduling

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.scheduling.ArcanaTask
import com.keopsfenks.arcana.core.scheduling.TaskContext
import java.util.function.Consumer
import org.bukkit.Bukkit
import org.bukkit.World

/**
 * Tasks on the Folia region owning one chunk: blocks and world state.
 *
 * Always addresses the region by chunk coordinates rather than by [org.bukkit.Location],
 * so there is one code path regardless of how the caller named the place.
 *
 * Loaded only on Folia.
 */
class FoliaRegionTaskContext(
    private val plugin: ArcanaPlugin,
    private val registry: MutableSet<FoliaArcanaTask>,
    private val world: World,
    private val chunkX: Int,
    private val chunkZ: Int
) : TaskContext {
    private fun submit(repeating: Boolean, submitter: (FoliaArcanaTask) -> Unit): ArcanaTask {
        val task = FoliaArcanaTask(plugin, repeating, registry, true)
        registry.add(task)
        submitter(task)
        return task
    }

    override fun run(runnable: Runnable): ArcanaTask = submit(false) { task ->
        task.bind(
            Bukkit.getRegionScheduler().run(plugin, world, chunkX, chunkZ, task.wrap(runnable))
        )
    }

    override fun runLater(runnable: Runnable, ticksLater: Long): ArcanaTask {
        if (ticksLater <= 0) {
            return run(runnable)
        }

        return submit(false) { task ->
            task.bind(
                Bukkit.getRegionScheduler()
                    .runDelayed(plugin, world, chunkX, chunkZ, task.wrap(runnable), ticksLater)
            )
        }
    }

    override fun runTimer(runnable: Runnable, delay: Long, repeat: Long): ArcanaTask =
        submit(true) { task ->
            task.bind(
                Bukkit.getRegionScheduler().runAtFixedRate(
                    plugin,
                    world,
                    chunkX,
                    chunkZ,
                    task.wrap(runnable),
                    delay.coerceAtLeast(1),
                    repeat.coerceAtLeast(1)
                )
            )
        }

    override fun runTimer(runnable: Consumer<ArcanaTask>, delay: Long, repeat: Long): ArcanaTask =
        submit(true) { task ->
            task.bind(
                Bukkit.getRegionScheduler().runAtFixedRate(
                    plugin,
                    world,
                    chunkX,
                    chunkZ,
                    task.wrapSelf(runnable),
                    delay.coerceAtLeast(1),
                    repeat.coerceAtLeast(1)
                )
            )
        }
}
