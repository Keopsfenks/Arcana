package com.keopsfenks.arcana.core.skills.platform;
import com.keopsfenks.arcana.core.skills.SkillResult;
import java.util.List;
import java.util.concurrent.CompletionStage;
/** Public bounded aura scope for extension components; no runtime implementation import is required. */
public interface SkillAuraContext {
    SkillManagedHandle instance();
    SkillActor caster();
    int stacks();
    int charges();
    void own(AutoCloseable cleanup);
    CompletionStage<SkillResult> execute(List<CompiledSkillMechanic> hooks);
    AutoCloseable subscribeTicks(long intervalTicks, List<CompiledSkillMechanic> hooks);
    AutoCloseable subscribeEvent(SkillKey kind, List<CompiledSkillMechanic> hooks, List<CompiledSkillInterceptor> interceptors, SkillPredicate condition, int consumeCharge);
    CompletionStage<SkillResult> addCharges(int delta);
}
