package com.keopsfenks.arcana.skills.platform.execution

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.DefaultSkillEngine
import com.keopsfenks.arcana.skills.platform.DefaultSkillPlatform
import com.keopsfenks.arcana.skills.platform.legacy.LegacyInvocationAdapter
import com.keopsfenks.arcana.skills.platform.sources.DefinitionRevision
import com.keopsfenks.arcana.skills.platform.values.*
import java.util.concurrent.CompletableFuture

class PlatformRunner(private val engine: DefaultSkillEngine, private val platform: DefaultSkillPlatform) {
    fun run(revision: DefinitionRevision, input: SkillInvocation, state: PlatformExecutionState, depth: Int, parent: InvocationFrame? = null): CompletableFuture<SkillResult> {
        if (depth > 16) return CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.BUDGET_EXCEEDED, "Skill calls exceed 16 levels"))
        return try {
            state.checkActive()
            if (!revision.available()) throw PlatformDependencyException("Definition ${revision.key} is unavailable")
            state.root.dependencies.addAll(revision.dependencies)
            state.root.dependencies.add(revision)
            state.checkActive()
            val native = revision.native
            if (native == null) {
                val context = LegacyInvocationAdapter.toLegacy(input)
                return engine.runLegacyNested(requireNotNull(revision.legacy), context, state.root, depth, parent).toCompletableFuture()
            }
            val parameters = ParameterBindings.bind(native.parameters, input.parameters())
            val supplied = input.derive().parameters(parameters).build()
            InvocationSnapshots.capture(supplied, engine.scheduler, state).thenCompose { snapshot ->
                val frame = state.frame(parent?.fork(snapshot) ?: InvocationFrame(snapshot))
                val persistent = native.bindings.filter { it.persistent }.groupBy { it.scope }.mapValues { entry -> entry.value.map { it.key }.toSet() }
                val variables = platform.variables.access(frame, persistent)
                variables.prepare().thenCompose {
                    val execution = PlatformExecutionContext(engine, platform, state, frame, native, depth, variables)
                    state.track(engine.scheduler.on(snapshot.actor().entity()) {
                        state.checkActive(); frame.checkActive()
                        when {
                            !native.enabled -> SkillResult.of(SkillResult.Status.DISABLED, "Skill is disabled")
                            !revision.available() -> SkillResult.of(SkillResult.Status.DEPENDENCY_UNAVAILABLE, "Definition ${revision.key} is unavailable")
                            native.permission != null && snapshot.actor().player().isEmpty -> SkillResult.of(SkillResult.Status.UNSUPPORTED_ACTOR, "Permission admission requires a Player actor")
                            native.permission != null && !snapshot.actor().player().orElseThrow().hasPermission(native.permission) -> SkillResult.of(SkillResult.Status.NO_PERMISSION, native.permission)
                            !native.conditions.test(execution.invocation()) -> SkillResult.of(SkillResult.Status.CONDITION_FAILED, "Conditions did not match")
                            native.chance == 0.0 || native.chance < 100.0 && checkedRoll() * 100 >= native.chance -> SkillResult.of(SkillResult.Status.CHANCE_FAILED, "Chance did not succeed")
                            !engine.cooldowns.tryAcquire(snapshot.actor().id(), native.cooldownGroup.toString(), native.cooldownNanos) -> SkillResult.of(SkillResult.Status.COOLDOWN, "Skill cooldown is active")
                            else -> SkillResult.success()
                        }
                    }).thenCompose { admission ->
                        if (admission.isSuccess) execution.execute(native.effects).toCompletableFuture() else CompletableFuture.completedFuture(admission)
                    }
                }.whenComplete { _, _ -> state.release(frame) }
            }
        } catch (error: Throwable) { CompletableFuture.completedFuture(PlatformExecutionState.failure(error)) }
    }
    private fun checkedRoll() = engine.random().also { require(it.isFinite() && it >= 0 && it < 1) { "Random source must return a finite value in [0, 1)" } }
}
