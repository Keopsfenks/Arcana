package com.keopsfenks.arcana.core.skills;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import org.bukkit.plugin.Plugin;

/** Entry point for explicit casts, custom triggers and owned component extensions. */
public interface SkillEngine {
    CompletionStage<SkillResult> cast(String skillId, SkillContext context);
    CompletionStage<List<SkillResult>> trigger(String triggerId, SkillContext context);
    SkillRegistration registerEffect(Plugin owner, SkillEffect effect);
    SkillRegistration registerCondition(Plugin owner, SkillCondition condition);
    SkillRegistration registerTrigger(Plugin owner, SkillTrigger trigger);
    SkillReloadResult reload();
    List<SkillInfo> skills();
    void cancel(UUID playerId);
    void shutdown();
}
