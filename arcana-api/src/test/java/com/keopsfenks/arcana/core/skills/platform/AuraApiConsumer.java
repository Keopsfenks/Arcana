package com.keopsfenks.arcana.core.skills.platform;
import com.keopsfenks.arcana.core.skills.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
/** Compilation fixture: a component author depends on the public API only. */
public final class AuraApiConsumer extends SkillAuraComponent {
    public AuraApiConsumer() { super(SkillDescriptor.of("custom_aura", "Public API extension fixture")); }
    @Override public CompiledSkillMechanic compile(SkillArguments args, SkillCompilation compiler) {
        List<CompiledSkillMechanic> hooks = compiler.effects(args.list("effects"), args.getPath() + ".effects");
        return execution -> {
            SkillAuraContext aura = execution.services().require(SkillAuraContext.class);
            SkillCleanup cleanup = execution.services().require(SkillCleanup.class);
            SkillActor actor = execution.invocation().actor();
            aura.own(() -> cleanup.on(actor, entity -> SkillResult.success()));
            aura.subscribeTicks(20, hooks);
            return CompletableFuture.completedFuture(SkillResult.success());
        };
    }
}
