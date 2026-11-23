package com.keopsfenks.arcana.core.skills;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

/** Safe scheduling and bounded flow control shared by built-in and external effects. */
public interface SkillExecution {
    SkillContext getContext();
    CompletionStage<SkillResult> onTarget(SkillTarget target, Function<LivingEntity, SkillResult> action);
    CompletionStage<SkillResult> atTarget(SkillTarget target, Function<Location, SkillResult> action);
    CompletionStage<SkillResult> global(Runnable action);
    CompletionStage<SkillResult> execute(List<CompiledSkillEffect> effects);
    CompletionStage<SkillResult> delay(long ticks);
    CompletionStage<SkillResult> cast(String skillId);
    String format(String text);
}
