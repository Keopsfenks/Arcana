package com.keopsfenks.arcana.core.skills.platform;

import com.keopsfenks.arcana.core.skills.*;
import org.bukkit.entity.Player;
import java.util.concurrent.CompletionStage;

/** This consumer intentionally uses only the original Player API. */
public final class LegacyConsumer extends SkillEffect {
    public LegacyConsumer() { super("legacy_consumer"); }

    @Override
    public CompiledSkillEffect compile(SkillArguments arguments, SkillCompiler compiler) {
        return execution -> execution.onTarget(SkillTarget.CASTER, entity -> SkillResult.success());
    }

    public static CompletionStage<SkillResult> cast(SkillEngine engine, Player player) {
        return engine.cast("legacy", SkillContext.builder(player).value("damage", 2).build());
    }
}
