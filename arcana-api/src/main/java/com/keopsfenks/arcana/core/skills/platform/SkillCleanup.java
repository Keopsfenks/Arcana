package com.keopsfenks.arcana.core.skills.platform;
import com.keopsfenks.arcana.core.skills.SkillResult;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import org.bukkit.entity.LivingEntity;
/** Host-owned scheduling for removing a lease's native state after its invocation has ended.
 * This service retains no invocation or event. Cleanup must remove only the caller's own state;
 * retired entities can produce NO_TARGET/CANCELLED rather than an unsafe off-owner fallback.
 */
@FunctionalInterface
public interface SkillCleanup {
    CompletionStage<SkillResult> on(SkillActor actor, Function<LivingEntity, SkillResult> cleanup);
}
