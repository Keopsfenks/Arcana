package com.keopsfenks.arcana.core.scheduling;

import org.jetbrains.annotations.NotNull;

/**
 * A runnable that can submit itself to a {@link Scheduler}.
 *
 * @deprecated Use {@link Scheduler} directly. A task that needs to cancel itself can use
 *         {@link TaskContext#runTimer(java.util.function.Consumer, long, long)}, which
 *         receives its own {@link ArcanaTask}. This type has no way to say which region owns
 *         the data it touches, so it is always scheduled globally.
 */
@Deprecated(forRemoval = true)
public interface RunnableTask extends Runnable, LegacyRunnableTask {
    /**
     * Run the task on the next tick.
     *
     * @return The task handle.
     */
    @Override
    @NotNull ArcanaTask runTask();

    /**
     * Run the task off-thread.
     *
     * @return The task handle.
     */
    @Override
    @NotNull ArcanaTask runTaskAsynchronously();

    /**
     * Run the task after a delay.
     *
     * @param delay The delay, in ticks.
     * @return The task handle.
     */
    @Override
    @NotNull ArcanaTask runTaskLater(long delay);

    /**
     * Run the task off-thread after a delay.
     *
     * @param delay The delay, in ticks.
     * @return The task handle.
     */
    @Override
    @NotNull ArcanaTask runTaskLaterAsynchronously(long delay);

    /**
     * Run the task repeatedly.
     *
     * @param delay  The delay before the first run, in ticks.
     * @param period The period between runs, in ticks.
     * @return The task handle.
     */
    @Override
    @NotNull ArcanaTask runTaskTimer(long delay,
                                  long period);

    /**
     * Run the task off-thread repeatedly.
     *
     * @param delay  The delay before the first run, in ticks.
     * @param period The period between runs, in ticks.
     * @return The task handle.
     */
    @Override
    @NotNull ArcanaTask runTaskTimerAsynchronously(long delay,
                                                long period);
}
