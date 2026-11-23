package com.keopsfenks.arcana.core.entities.ai.entity;

import com.keopsfenks.arcana.core.config.interfaces.Config;
import com.keopsfenks.arcana.core.entities.ai.EntityGoal;
import com.keopsfenks.arcana.core.serialization.KeyedDeserializer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

/**
 * Allows an entity to move to water when on land.
 */
public record EntityGoalTryFindWater(
) implements EntityGoal<Mob> {
    /**
     * The deserializer for the goal.
     */
    public static final KeyedDeserializer<EntityGoalTryFindWater> DESERIALIZER = new EntityGoalTryFindWater.Deserializer();

    /**
     * Deserialize configs into the goal.
     */
    private static final class Deserializer implements KeyedDeserializer<EntityGoalTryFindWater> {
        @Override
        public EntityGoalTryFindWater deserialize(@NotNull final Config config) {
            return new EntityGoalTryFindWater();
        }

        @NotNull
        @Override
        public NamespacedKey getKey() {
            return NamespacedKey.minecraft("try_find_water");
        }
    }
}
