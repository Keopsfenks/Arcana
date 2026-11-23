package com.keopsfenks.arcana.core.entities.ai.target;

import com.keopsfenks.arcana.core.config.interfaces.Config;
import com.keopsfenks.arcana.core.entities.Entities;
import com.keopsfenks.arcana.core.entities.TestableEntity;
import com.keopsfenks.arcana.core.entities.ai.TargetGoal;
import com.keopsfenks.arcana.core.serialization.KeyedDeserializer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Allows an entity to target whatever hurt it.
 *
 * @param blacklist The entities not to target when hurt by them.
 */
@SuppressWarnings({"varargs"})
public record TargetGoalHurtBy(
        @NotNull TestableEntity blacklist
) implements TargetGoal<Mob> {
    /**
     * The deserializer for the goal.
     */
    public static final KeyedDeserializer<TargetGoalHurtBy> DESERIALIZER = new TargetGoalHurtBy.Deserializer();

    /**
     * Deserialize configs into the goal.
     */
    private static final class Deserializer implements KeyedDeserializer<TargetGoalHurtBy> {
        @Override
        @Nullable
        public TargetGoalHurtBy deserialize(@NotNull final Config config) {
            if (!(
                    config.has("blacklist")
            )) {
                return null;
            }

            return new TargetGoalHurtBy(
                    Entities.lookup(config.getString("blacklist"))
            );
        }

        @NotNull
        @Override
        public NamespacedKey getKey() {
            return NamespacedKey.minecraft("hurt_by");
        }
    }
}
