package com.keopsfenks.arcana.core.entities.ai.entity;

import com.keopsfenks.arcana.core.config.interfaces.Config;
import com.keopsfenks.arcana.core.entities.ai.EntityGoal;
import com.keopsfenks.arcana.core.serialization.KeyedDeserializer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Will make the entity actively avoid the sunlight.
 *
 * @param speed The speed at which to flee.
 */
public record EntityGoalFleeSun(
        double speed
) implements EntityGoal<Mob> {
    /**
     * The deserializer for the goal.
     */
    public static final KeyedDeserializer<EntityGoalFleeSun> DESERIALIZER = new EntityGoalFleeSun.Deserializer();

    /**
     * Deserialize configs into the goal.
     */
    private static final class Deserializer implements KeyedDeserializer<EntityGoalFleeSun> {
        @Override
        @Nullable
        public EntityGoalFleeSun deserialize(@NotNull final Config config) {
            if (!(
                    config.has("speed")
            )) {
                return null;
            }

            return new EntityGoalFleeSun(
                    config.getDouble("speed")
            );
        }

        @NotNull
        @Override
        public NamespacedKey getKey() {
            return NamespacedKey.minecraft("flee_sun");
        }
    }
}
