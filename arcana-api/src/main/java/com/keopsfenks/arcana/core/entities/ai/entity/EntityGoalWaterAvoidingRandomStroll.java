package com.keopsfenks.arcana.core.entities.ai.entity;

import com.keopsfenks.arcana.core.config.interfaces.Config;
import com.keopsfenks.arcana.core.entities.ai.EntityGoal;
import com.keopsfenks.arcana.core.serialization.KeyedDeserializer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stroll randomly while avoiding water.
 *
 * @param speed  The speed.
 * @param chance The chance to stroll every tick, between 0 and 1.
 */
public record EntityGoalWaterAvoidingRandomStroll(
        double speed,
        double chance
) implements EntityGoal<Mob> {
    /**
     * The deserializer for the goal.
     */
    public static final KeyedDeserializer<EntityGoalWaterAvoidingRandomStroll> DESERIALIZER = new EntityGoalWaterAvoidingRandomStroll.Deserializer();

    /**
     * Deserialize configs into the goal.
     */
    private static final class Deserializer implements KeyedDeserializer<EntityGoalWaterAvoidingRandomStroll> {
        @Override
        @Nullable
        public EntityGoalWaterAvoidingRandomStroll deserialize(@NotNull final Config config) {
            if (!(
                    config.has("speed")
                            && config.has("chance")
            )) {
                return null;
            }

            return new EntityGoalWaterAvoidingRandomStroll(
                    config.getDouble("speed"),
                    config.getDouble("chance")
            );
        }

        @NotNull
        @Override
        public NamespacedKey getKey() {
            return NamespacedKey.minecraft("water_avoiding_random_stroll");
        }
    }
}
