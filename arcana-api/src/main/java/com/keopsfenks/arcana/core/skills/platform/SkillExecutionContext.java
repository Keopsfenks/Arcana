package com.keopsfenks.arcana.core.skills.platform;

import com.keopsfenks.arcana.core.skills.SkillResult;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

/** Scheduler, bounded execution and invocation-scoped services for a compiled mechanic. */
public interface SkillExecutionContext {
    SkillInvocation invocation();
    CompletionStage<SkillResult> on(SkillActor actor, Function<LivingEntity, SkillResult> action);
    CompletionStage<SkillResult> at(SkillLocation location, Function<Location, SkillResult> action);
    CompletionStage<SkillResult> execute(List<CompiledSkillMechanic> mechanics);
    CompletionStage<SkillResult> delay(long ticks);
    CompletionStage<SkillResult> cast(SkillKey key, Map<String, SkillValue> parameters);
    SkillServices services();
}
