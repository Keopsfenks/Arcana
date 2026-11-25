package com.keopsfenks.arcana.skills.platform.events
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.DefaultSkillEngine
import com.keopsfenks.arcana.skills.execution.ExecutionState
import com.keopsfenks.arcana.skills.execution.SkillDamageScope
import com.keopsfenks.arcana.skills.platform.DefaultSkillPlatform
import com.keopsfenks.arcana.skills.platform.execution.*
import com.keopsfenks.arcana.skills.platform.lifecycle.SkillLifecycleGate
import com.keopsfenks.arcana.skills.platform.providers.StatBindings
import com.keopsfenks.arcana.skills.platform.values.*
import org.bukkit.event.Event

data class InterceptionOutcome(val result: SkillResult, val continuation: AdmittedContinuation? = null)
class InterceptorDispatcher(private val engine: DefaultSkillEngine, private val platform: DefaultSkillPlatform, private val lifecycle: SkillLifecycleGate) {
    fun intercept(key: SkillKey, input: SkillInvocation, event: Event): InterceptionOutcome {
        // Ownership probes precede *all* event/entity reads and all provider callbacks.
        if (!InvocationSnapshots.references(input).all { engine.scheduler.owns(it.entity()) }) return failed(SkillResult.Status.UNSUPPORTED_CONTEXT, "All required entities must be locally owned")
        if (SkillDamageScope.isActive()) return failed(SkillResult.Status.CANCELLED, "Generated skill damage")
        val snapshot = platform.repository.snapshot()
        val revision = snapshot.entries[key] ?: return failed(SkillResult.Status.UNKNOWN_SKILL, "Unknown skill $key")
        val definition = revision.native ?: return failed(SkillResult.Status.UNSUPPORTED_CONTEXT, "Legacy definition has no interceptor phase")
        if (!definition.enabled) return failed(SkillResult.Status.DISABLED, "Skill is disabled")
        if (definition.interceptors.isEmpty()) return failed(SkillResult.Status.UNSUPPORTED_CONTEXT, "Definition has no interceptors")
        var root: ExecutionState? = null
        ScopedEventAccess(event).use { access ->
            try {
                if (access.cancelled()) return failed(SkillResult.Status.CANCELLED, "Event was already cancelled")
                val state = lifecycle.locked {
                    check(platform.repository.snapshot().entries[key] === revision && revision.available()) { "Definition changed" }
                    val created = ExecutionState(input.actor().id(), snapshot.legacy)
                    root = created; created.dependencies.add(revision); created.dependencies.addAll(revision.dependencies)
                    check(engine.enroll(created)) { "Skill engine stopped" }; platform.attach(created)
                }
                val parameters = ParameterBindings.bind(definition.parameters, input.parameters())
                val captured = InvocationSnapshots.captureLocal(input.derive().parameters(parameters).build(), engine.scheduler, definition.snapshots, platform)
                val enriched = StatBindings.captureLocal(captured, definition.stats, platform)
                val frame = state.frame(InvocationFrame(enriched)); val variables = platform.variables.access(frame)
                val context = PlatformExecutionContext(engine, platform, state, frame, definition, 1, variables)
                val invocation = context.invocation()
                val permission = definition.permission
                if (permission != null && invocation.actor().player().isEmpty) return reject(root, SkillResult.Status.UNSUPPORTED_ACTOR, "Permission requires Player")
                if (permission != null && !invocation.actor().player().orElseThrow().hasPermission(permission)) return reject(root, SkillResult.Status.NO_PERMISSION, permission)
                if (!definition.conditions.test(invocation)) return reject(root, SkillResult.Status.CONDITION_FAILED, "Conditions did not match")
                val chance = definition.chance
                if (chance == 0.0 || chance < 100 && checkedRoll() * 100 >= chance) return reject(root, SkillResult.Status.CHANCE_FAILED, "Chance did not succeed")
                val pending = platform.admissions.admit(revision, context, local = true)
                check(pending.isDone) { "Local admission attempted asynchronous work" }
                val admitted = pending.join()
                if (!admitted.result.isSuccess) { root!!.result.complete(admitted.result); return InterceptionOutcome(admitted.result) }
                context.admission = admitted.admission
                for ((index, interceptor) in definition.interceptors.withIndex()) {
                    if (index >= 64) return reject(root, SkillResult.Status.BUDGET_EXCEEDED, "Interception exceeds 64 local steps")
                    state.step(); frame.checkActive()
                    val result = interceptor.apply(access, invocation, requireNotNull(admitted.admission))
                    if (!result.isSuccess) { root!!.result.complete(result); return InterceptionOutcome(result) }
                }
                return InterceptionOutcome(SkillResult.success(), AdmittedContinuation(context, revision))
            } catch (error: Throwable) {
                val result = PlatformExecutionState.failure(error); root?.result?.complete(result); return InterceptionOutcome(result)
            }
        }
    }
    private fun checkedRoll() = engine.random().also { require(it.isFinite() && it >= 0 && it < 1) }
    private fun failed(status: SkillResult.Status, message: String) = InterceptionOutcome(SkillResult.of(status, message))
    private fun reject(root: ExecutionState?, status: SkillResult.Status, message: String): InterceptionOutcome = failed(status, message).also { root?.result?.complete(it.result) }
}
