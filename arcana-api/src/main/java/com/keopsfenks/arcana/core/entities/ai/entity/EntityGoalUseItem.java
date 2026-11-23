package com.keopsfenks.arcana.core.entities.ai.entity;

import com.keopsfenks.arcana.core.config.interfaces.Config;
import com.keopsfenks.arcana.core.entities.Entities;
import com.keopsfenks.arcana.core.entities.TestableEntity;
import com.keopsfenks.arcana.core.entities.ai.EntityGoal;
import com.keopsfenks.arcana.core.items.Items;
import com.keopsfenks.arcana.core.serialization.KeyedDeserializer;
import com.keopsfenks.arcana.util.SoundUtils;
import java.util.function.Predicate;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Use item.
 *
 * @param item      The item.
 * @param sound     The sound to play on use.
 * @param condition The condition when to use the item.
 */
public record EntityGoalUseItem(
        @NotNull ItemStack item,
        @NotNull Sound sound,
        @NotNull Predicate<LivingEntity> condition
) implements EntityGoal<Mob> {
    /**
     * The deserializer for the goal.
     */
    public static final KeyedDeserializer<EntityGoalUseItem> DESERIALIZER = new EntityGoalUseItem.Deserializer();

    /**
     * Deserialize configs into the goal.
     */
    private static final class Deserializer implements KeyedDeserializer<EntityGoalUseItem> {
        @Override
        @Nullable
        public EntityGoalUseItem deserialize(@NotNull final Config config) {
            if (!(
                    config.has("item")
                            && config.has("sound")
                            && config.has("condition")
            )) {
                return null;
            }

            TestableEntity filter = Entities.lookup(config.getString("condition"));

            Sound sound = SoundUtils.getSound(config.getString("sound"));

            if (sound == null) {
                return null;
            }

            return new EntityGoalUseItem(
                    Items.lookup(config.getString("item")).getItem(),
                    sound,
                    filter::matches
            );
        }

        @NotNull
        @Override
        public NamespacedKey getKey() {
            return NamespacedKey.minecraft("use_item");
        }
    }
}
