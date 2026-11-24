package com.keopsfenks.arcana.skills.platform.execution

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.DefaultSkillEngine
import com.keopsfenks.arcana.skills.platform.DefaultSkillPlatform
import com.keopsfenks.arcana.skills.platform.compile.CompiledPlatformSkill
import com.keopsfenks.arcana.skills.platform.compile.CompiledNode
import com.keopsfenks.arcana.skills.platform.values.InvocationFrame
import com.keopsfenks.arcana.skills.platform.values.VariableStore
import com.keopsfenks.arcana.skills.platform.targets.RegionQuery
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import java.util.concurrent.*
import java.util.function.Function
import java.util.function.DoubleSupplier
import com.keopsfenks.arcana.skills.execution.ScheduledWork
import com.keopsfenks.arcana.skills.platform.providers.StatBindings

class PlatformExecutionContext(
    val engine: DefaultSkillEngine,
    val platform: DefaultSkillPlatform,
    val state: PlatformExecutionState,
    val frame: InvocationFrame,
    val definition: CompiledPlatformSkill,
    val depth: Int,
    val variables: VariableStore.VariableAccess,
    initialAdmission: SkillAdmission? = null
) : SkillExecutionContext {
    var admission: SkillAdmission? = initialAdmission
        internal set
    private val query: SkillTargetQuery = RegionQuery(engine.scheduler, state = state, frame = frame)
    private val flow = ParallelScope(this)
    fun <T> track(work: ScheduledWork<T>): CompletableFuture<T> = state.track(frame.track(work))
    override fun invocation(): SkillInvocation { state.checkActive(); return frame.snapshot(definition.bindings, variables) }
    override fun services(): SkillServices = SkillServices.of(buildMap<Class<*>, Any> {
        put(SkillVariables::class.java, variables); put(SkillTargetQuery::class.java, query); put(ParallelScope::class.java, flow)
        put(FlowCursorStore::class.java, platform.flowCursors); put(DoubleSupplier::class.java, DoubleSupplier { engine.random() })
        admission?.let { put(SkillAdmission::class.java, it) }
        state.managedState?.let { put(com.keopsfenks.arcana.skills.platform.managed.ManagedExecutionState::class.java, it) }
        put(com.keopsfenks.arcana.skills.platform.auras.AuraManager::class.java, platform.auras.forContext(this@PlatformExecutionContext))
    })
    fun derived(input: SkillInvocation, isolateLocals: Boolean = false): CompletableFuture<PlatformExecutionContext> = InvocationSnapshots.capture(input, engine.scheduler, state, frame)
        .thenCompose { StatBindings.capture(it, definition.stats, state, frame, platform) }.thenCompose { captured ->
        val view = state.frame(if (isolateLocals) frame.fork(captured) else frame.view(captured))
        prepare(view)
    }
    private fun prepare(view: InvocationFrame): CompletableFuture<PlatformExecutionContext> {
        try {
            val persistent = definition.bindings.filter { it.persistent }.groupBy { it.scope }.mapValues { it.value.map { value -> value.key }.toSet() }
            val access = platform.variables.access(view, persistent)
            return access.prepare().thenApply { PlatformExecutionContext(engine, platform, state, view, definition, depth, access, admission) }
                .whenComplete { _, error -> if (error != null) state.release(view) }
        } catch (error: Throwable) { state.release(view); return CompletableFuture.failedFuture(error) }
    }
    fun branch(input: SkillInvocation): PlatformBranch {
        val scope = state.frame(frame.fork(input))
        val ready = InvocationSnapshots.capture(input, engine.scheduler, state, scope)
            .thenCompose { StatBindings.capture(it, definition.stats, state, scope, platform) }
            .thenCompose { captured -> prepare(state.frame(scope.view(captured))) }
        return PlatformBranch(scope, ready)
    }
    override fun on(actor: SkillActor, action: Function<LivingEntity, SkillResult>): CompletionStage<SkillResult> =
        track(engine.scheduler.on(actor.entity()) {
            state.checkActive(); frame.checkActive()
            if (!InvocationSnapshots.valid(actor.entity())) SkillResult.of(SkillResult.Status.NO_TARGET, "Actor target is unavailable") else action.apply(actor.entity())
        }).handle { value, error ->
            state.checkActive(); frame.checkActive()
            if (error == null) value else if (PlatformExecutionState.failure(error).status() == SkillResult.Status.CANCELLED && actor != frame.invocation.actor())
                SkillResult.of(SkillResult.Status.NO_TARGET, "Selected actor retired") else throw CompletionException(error)
        }
    override fun at(location: SkillLocation, action: Function<Location, SkillResult>): CompletionStage<SkillResult> =
        track(engine.scheduler.global { state.checkActive(); frame.checkActive(); Bukkit.getWorld(location.world()) }).thenCompose { world ->
            if (world == null) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.NO_TARGET, "World is unavailable"))
            else {
                val target = Location(world, location.x(), location.y(), location.z(), location.yaw(), location.pitch())
                track(engine.scheduler.at(target) { state.checkActive(); frame.checkActive(); action.apply(target) })
            }
        }
    override fun execute(mechanics: List<CompiledSkillMechanic>): CompletionStage<SkillResult> {
        val immutable = java.util.List.copyOf(mechanics)
        // Schedule a bounded drain instead of composing a synchronous 1024-deep future chain.
        val result = CompletableFuture<SkillResult>()
        fun drain(index: Int, initial: SkillResult) {
            try {
                state.checkActive(); frame.checkActive()
                var cursor = index
                var last = initial
                while (cursor < immutable.size && !result.isDone) {
                    val mechanic = immutable[cursor++]
                    if (mechanic !is CompiledNode) state.step()
                    val next = mechanic.execute(this).toCompletableFuture()
                    if (next.isDone) {
                        val outcome = next.join()
                        if (!outcome.isSuccess) { result.complete(outcome); return }
                        last = outcome
                    } else {
                        val resume = cursor
                        next.whenComplete { outcome, error ->
                            if (error != null) result.completeExceptionally(error)
                            else if (!outcome.isSuccess) result.complete(outcome)
                            else drain(resume, outcome)
                        }
                        return
                    }
                }
                if (!result.isDone) result.complete(last)
            } catch (error: Throwable) { result.completeExceptionally(error) }
        }
        drain(0, SkillResult.success())
        return result
    }
    override fun delay(ticks: Long): CompletionStage<SkillResult> {
        require(ticks in 0..1_728_000L) { "Delay must be between zero and 24 hours" }
        return track(engine.scheduler.on(frame.invocation.actor().entity(), ticks) { state.checkActive(); frame.checkActive(); SkillResult.success() })
    }
    override fun cast(key: SkillKey, parameters: Map<String, SkillValue>): CompletionStage<SkillResult> =
        platform.runNested(key, invocation().derive().parameters(parameters).build(), state, depth + 1, frame)
}
