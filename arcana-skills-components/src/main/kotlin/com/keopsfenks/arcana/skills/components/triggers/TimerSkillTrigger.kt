package com.keopsfenks.arcana.skills.components.triggers
import com.keopsfenks.arcana.core.skills.platform.*
import java.util.function.Consumer
class TimerSkillTrigger : SkillPlatformTrigger(SkillDescriptor.of("timer", "Explicit owned recipient timer"), emptySet()) {
    override fun supportsTimerSubscriptions() = true
    override fun listen(consumer: Consumer<SkillInvocation>) = AutoCloseable { }
}
