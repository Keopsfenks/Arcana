package com.keopsfenks.arcana.core.factory;

import com.keopsfenks.arcana.core.scheduling.RunnableTask;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

/**
 * Factory to create runnables.
 *
 * @deprecated Use {@link com.keopsfenks.arcana.core.scheduling.Scheduler} directly.
 */
@Deprecated(forRemoval = true)
public interface RunnableFactory {
    /**
     * Create a {@link RunnableTask}.
     *
     * @param consumer Lambda of the code to run, where the parameter represents the instance of the runnable.
     * @return The created {@link RunnableTask}.
     */
    RunnableTask create(@NotNull Consumer<RunnableTask> consumer);
}
