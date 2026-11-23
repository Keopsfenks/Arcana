package com.keopsfenks.arcana.core.entities.ai.entity;

import com.keopsfenks.arcana.core.config.interfaces.Config;
import com.keopsfenks.arcana.core.entities.ai.EntityGoal;
import com.keopsfenks.arcana.core.serialization.KeyedDeserializer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Cat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Allows a cat to sit on a block, such as a bed, a chest, or a lit furnace.
 *
 * @param speed The speed at which to move to the block.
 */
public record EntityGoalCatSitOnBed(
        double speed
) implements EntityGoal<Cat> {
    /**
     * The deserializer for the goal.
     */
    public static final KeyedDeserializer<EntityGoalCatSitOnBed> DESERIALIZER = new EntityGoalCatSitOnBed.Deserializer();

    /**
     * Deserialize configs into the goal.
     */
    private static final class Deserializer implements KeyedDeserializer<EntityGoalCatSitOnBed> {
        @Override
        @Nullable
        public EntityGoalCatSitOnBed deserialize(@NotNull final Config config) {
            if (!(
                    config.has("speed")
            )) {
                return null;
            }

            return new EntityGoalCatSitOnBed(
                    config.getDouble("speed")
            );
        }

        @NotNull
        @Override
        public NamespacedKey getKey() {
            return NamespacedKey.minecraft("cat_sit_on_bed");
        }
    }
}
