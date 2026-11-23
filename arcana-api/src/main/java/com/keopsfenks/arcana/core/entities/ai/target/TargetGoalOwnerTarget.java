package com.keopsfenks.arcana.core.entities.ai.target;

import com.keopsfenks.arcana.core.config.interfaces.Config;
import com.keopsfenks.arcana.core.entities.ai.TargetGoal;
import com.keopsfenks.arcana.core.serialization.KeyedDeserializer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Tameable;
import org.jetbrains.annotations.NotNull;

/**
 * Allows a tameable entity to target whatever its owner hurt.
 */
public record TargetGoalOwnerTarget(
) implements TargetGoal<Tameable> {
    /**
     * The deserializer for the goal.
     */
    public static final KeyedDeserializer<TargetGoalOwnerTarget> DESERIALIZER = new TargetGoalOwnerTarget.Deserializer();

    /**
     * Deserialize configs into the goal.
     */
    private static final class Deserializer implements KeyedDeserializer<TargetGoalOwnerTarget> {
        @Override
        public TargetGoalOwnerTarget deserialize(@NotNull final Config config) {
            return new TargetGoalOwnerTarget();
        }

        @NotNull
        @Override
        public NamespacedKey getKey() {
            return NamespacedKey.minecraft("owner_target");
        }
    }
}
