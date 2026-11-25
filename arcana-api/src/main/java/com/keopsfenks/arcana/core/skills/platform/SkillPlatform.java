package com.keopsfenks.arcana.core.skills.platform;

import com.keopsfenks.arcana.core.skills.SkillRegistration;
import com.keopsfenks.arcana.core.skills.SkillResult;
import com.keopsfenks.arcana.core.skills.SkillReloadResult;
import java.util.List;
import java.util.concurrent.CompletionStage;
import org.bukkit.plugin.Plugin;

/** Owned extension and general-actor entry point, alongside the original Player engine API. */
public interface SkillPlatform {
    /** Explicit bounded recipient subscription; declaring a YAML timer never scans online entities. */
    default SkillManagedHandle subscribeTimer(org.bukkit.plugin.Plugin owner, SkillKey trigger, SkillActor actor, long intervalTicks) {
        throw new UnsupportedOperationException("Timer subscriptions unavailable");
    }
    CompletionStage<SkillResult> cast(SkillKey key, SkillInvocation invocation);
    CompletionStage<List<SkillResult>> trigger(SkillKey key, SkillInvocation invocation);
    <T extends SkillPlatformComponent> SkillRegistration register(Plugin owner, SkillComponentType<T> type, SkillKey key, T component);
    SkillRegistration registerSource(Plugin owner, SkillDefinitionSource source);
    SkillReloadResult reloadSource(SkillKey source);
    List<SkillDefinitionInfo> definitions();
    default CompletionStage<List<SkillHolder>> refreshHolders(SkillActor actor) { return java.util.concurrent.CompletableFuture.completedFuture(List.of()); }
    default List<SkillHolder> holders(SkillActor actor) { return List.of(); }
}
