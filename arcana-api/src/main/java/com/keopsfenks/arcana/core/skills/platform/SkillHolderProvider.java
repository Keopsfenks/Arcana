package com.keopsfenks.arcana.core.skills.platform;
import java.util.List;
import java.util.concurrent.CompletionStage;
/** Called only for explicitly tracked actors. Providers must capture live state on its owner. */
public interface SkillHolderProvider extends SkillPlatformComponent {
    CompletionStage<List<SkillHolder>> holders(SkillActor actor);
}
