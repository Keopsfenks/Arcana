package com.keopsfenks.arcana.skills.execution

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.skills.DefaultSkillEngine
import com.keopsfenks.arcana.skills.compile.CompiledSkill
import java.util.concurrent.CompletableFuture

class SkillRunner(private val engine: DefaultSkillEngine) {
    fun run(skill: CompiledSkill, context: SkillContext, state: ExecutionState, depth: Int, platformFrame: com.keopsfenks.arcana.skills.platform.values.InvocationFrame? = null): CompletableFuture<SkillResult> {
        if (depth > 16) return completed(SkillResult.Status.ERROR, "Skill calls exceed 16 levels")
        state.dependencies.addAll(skill.dependencies)
        return engine.scheduler.on(context.caster) {
            state.checkActive(); platformFrame?.checkActive()
            when {
                !skill.enabled -> SkillResult.of(SkillResult.Status.DISABLED, "Skill is disabled")
                !skill.available() -> SkillResult.of(SkillResult.Status.DEPENDENCY_UNAVAILABLE, "A component dependency is unavailable")
                skill.permission != null && !context.caster.hasPermission(skill.permission) -> SkillResult.of(SkillResult.Status.NO_PERMISSION, skill.permission)
                !skill.filters.test(context) -> SkillResult.of(SkillResult.Status.FILTER_FAILED, "Trigger filters did not match")
                !skill.conditions.test(context) -> SkillResult.of(SkillResult.Status.CONDITION_FAILED, "Conditions did not match")
                skill.chance == 0.0 || skill.chance < 100.0 && checkedRoll() * 100.0 >= skill.chance -> SkillResult.of(SkillResult.Status.CHANCE_FAILED, "Chance did not succeed")
                !engine.cooldowns.tryAcquire(context.casterId, skill.cooldownGroup, skill.cooldownNanos) ->
                    SkillResult.of(SkillResult.Status.COOLDOWN, "${engine.cooldowns.remaining(context.casterId, skill.cooldownGroup) / 1_000_000_000.0}s remaining")
                else -> SkillResult.success()
            }
        }.let { state.track(platformFrame?.track(it) ?: it) }.thenCompose { admission ->
            if (!admission.isSuccess) CompletableFuture.completedFuture(admission)
            else SkillExecutionImpl(engine, state, context.toBuilder().value("skill", skill.id).build(), depth, platformFrame).execute(skill.effects).toCompletableFuture()
        }
    }

    private fun checkedRoll(): Double = engine.random().also { require(it.isFinite() && it >= 0.0 && it < 1.0) { "Random source must return a finite value in [0, 1)" } }
    private fun completed(status: SkillResult.Status, message: String) = CompletableFuture.completedFuture(SkillResult.of(status, message))
}
