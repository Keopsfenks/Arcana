package com.keopsfenks.arcana.core.entities.ai.entity;

import com.keopsfenks.arcana.core.config.interfaces.Config;
import com.keopsfenks.arcana.core.entities.ai.EntityGoal;
import com.keopsfenks.arcana.core.serialization.KeyedDeserializer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Allows an entity to jump towards a target.
 *
 * @param velocity The leap velocity.
 */
public record EntityGoalLeapAtTarget(
        double velocity
) implements EntityGoal<Mob> {
    /**
     * The deserializer for the goal.
     */
    public static final KeyedDeserializer<EntityGoalLeapAtTarget> DESERIALIZER = new EntityGoalLeapAtTarget.Deserializer();

    /**
     * Deserialize configs into the goal.
     */
    private static final class Deserializer implements KeyedDeserializer<EntityGoalLeapAtTarget> {
        @Override
        @Nullable
        public EntityGoalLeapAtTarget deserialize(@NotNull final Config config) {
            if (!(
                    config.has("velocity")
            )) {
                return null;
            }

            return new EntityGoalLeapAtTarget(
                    config.getDouble("velocity")
            );
        }

        @NotNull
        @Override
        public NamespacedKey getKey() {
            return NamespacedKey.minecraft("leap_at_target");
        }
    }
}
