package com.keopsfenks.arcana.core.entities.ai.target;

import com.keopsfenks.arcana.core.config.interfaces.Config;
import com.keopsfenks.arcana.core.entities.ai.TargetGoal;
import com.keopsfenks.arcana.core.serialization.KeyedDeserializer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.IronGolem;
import org.jetbrains.annotations.NotNull;

/**
 * Allows an iron golem to defend a village by targeting entities that have attacked its villagers.
 */
public record TargetGoalDefendVillage(
) implements TargetGoal<IronGolem> {
    /**
     * The deserializer for the goal.
     */
    public static final KeyedDeserializer<TargetGoalDefendVillage> DESERIALIZER = new TargetGoalDefendVillage.Deserializer();

    /**
     * Deserialize configs into the goal.
     */
    private static final class Deserializer implements KeyedDeserializer<TargetGoalDefendVillage> {
        @Override
        public TargetGoalDefendVillage deserialize(@NotNull final Config config) {
            return new TargetGoalDefendVillage();
        }

        @NotNull
        @Override
        public NamespacedKey getKey() {
            return NamespacedKey.minecraft("defend_village");
        }
    }
}
