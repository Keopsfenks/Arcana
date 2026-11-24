package com.keopsfenks.arcana.skills.platform.execution

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.DefaultSkillEngine
import com.keopsfenks.arcana.skills.platform.DefaultSkillPlatform
import com.keopsfenks.arcana.skills.platform.legacy.LegacyInvocationAdapter
import com.keopsfenks.arcana.skills.platform.sources.DefinitionRevision
import com.keopsfenks.arcana.skills.platform.values.*
import java.util.concurrent.CompletableFuture
import com.keopsfenks.arcana.skills.platform.providers.StatBindings

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
            if (!native.enabled) return CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.DISABLED, "Skill is disabled"))
            val parameters = ParameterBindings.bind(native.parameters, input.parameters())
            val supplied = input.derive().parameters(parameters).build()
            InvocationSnapshots.capture(supplied, engine.scheduler, state, parent).thenCompose { snapshot ->
                val frame = state.frame(parent?.fork(snapshot) ?: InvocationFrame(snapshot))
                val persistent = native.bindings.filter { it.persistent }.groupBy { it.scope }.mapValues { entry -> entry.value.map { it.key }.toSet() }
                val variables = platform.variables.access(frame, persistent)
                variables.prepare().thenCompose { StatBindings.capture(snapshot, native.stats, state, frame, platform) }.thenCompose { enriched ->
                    val effectFrame = if (enriched === snapshot) frame else state.frame(frame.view(enriched))
                    val effectVariables = if (effectFrame === frame) variables else platform.variables.access(effectFrame, persistent)
                    effectVariables.prepare().thenCompose {
                        val execution = PlatformExecutionContext(engine, platform, state, effectFrame, native, depth, effectVariables)
                        execution.track(engine.scheduler.on(snapshot.actor().entity()) {
                            state.checkActive(); effectFrame.checkActive()
                            when {
                                !InvocationSnapshots.valid(snapshot.actor().entity()) -> SkillResult.of(SkillResult.Status.CANCELLED, "Actor retired")
                                !native.enabled -> SkillResult.of(SkillResult.Status.DISABLED, "Skill is disabled")
                                !revision.available() -> SkillResult.of(SkillResult.Status.DEPENDENCY_UNAVAILABLE, "Definition ${revision.key} is unavailable")
                                native.permission != null && snapshot.actor().player().isEmpty -> SkillResult.of(SkillResult.Status.UNSUPPORTED_ACTOR, "Permission admission requires a Player actor")
                                native.permission != null && !snapshot.actor().player().orElseThrow().hasPermission(native.permission) -> SkillResult.of(SkillResult.Status.NO_PERMISSION, native.permission)
                                !native.conditions.test(execution.invocation()) -> SkillResult.of(SkillResult.Status.CONDITION_FAILED, "Conditions did not match")
                                native.chance == 0.0 || native.chance < 100.0 && checkedRoll() * 100 >= native.chance -> SkillResult.of(SkillResult.Status.CHANCE_FAILED, "Chance did not succeed")
                                else -> SkillResult.success()
                            }
                        }).thenCompose { eligible ->
                            if (!eligible.isSuccess) CompletableFuture.completedFuture(eligible)
                            else platform.admissions.admit(revision, execution).thenCompose { outcome ->
                                if (!outcome.result.isSuccess) CompletableFuture.completedFuture(outcome.result)
                                else { execution.admission = outcome.admission; execution.execute(native.effects).toCompletableFuture() }
                            }
                        }
                    }
                }.whenComplete { _, _ -> state.release(frame) }
            }
        } catch (error: Throwable) { CompletableFuture.completedFuture(PlatformExecutionState.failure(error)) }
    }
    private fun checkedRoll() = engine.random().also { require(it.isFinite() && it >= 0 && it < 1) { "Random source must return a finite value in [0, 1)" } }
}
