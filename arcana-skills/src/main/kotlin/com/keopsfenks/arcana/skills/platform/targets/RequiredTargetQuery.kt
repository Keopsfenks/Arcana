package com.keopsfenks.arcana.skills.platform.targets
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.execution.SkillScheduler
import com.keopsfenks.arcana.skills.platform.DefaultSkillPlatform
import com.keopsfenks.arcana.skills.platform.execution.*
import com.keopsfenks.arcana.skills.platform.values.InvocationFrame
import com.keopsfenks.arcana.skills.platform.values.VariableBinding
import java.util.Optional
import java.util.concurrent.CompletableFuture

/** Pure filters receive fresh candidate bindings and declared owned metadata, including TARGET variables. */
class RequiredTargetQuery(private val delegate: SkillTargetQuery,
    private val requests: List<CompiledSnapshotRequest>, private val scheduler: SkillScheduler,
    private val state: PlatformExecutionState, private val frame: InvocationFrame, private val platform: DefaultSkillPlatform,
    private val bindings: List<VariableBinding>) : SkillTargetQuery {
    override fun loaded(bounds: SkillBounds) = delegate.loaded(bounds)
    override fun entities(bounds: SkillBounds) = delegate.entities(bounds)
    override fun collisions(from: SkillLocation, to: SkillLocation, radius: Double) = delegate.collisions(from, to, radius)
    override fun snapshot(actor: SkillActor) = delegate.snapshot(actor)
    override fun project(input: SkillInvocation, candidate: SkillEntitySnapshot) = enrich(TargetSelectionPolicy.selected(input, candidate)).thenApply { Optional.of(it) }
    override fun projectLocation(input: SkillInvocation, location: SkillLocation) = enrich(TargetSelectionPolicy.selected(input, location)).thenApply { Optional.of(it) }
    private fun enrich(input: SkillInvocation): CompletableFuture<SkillInvocation> {
        if (requests.isEmpty() && bindings.isEmpty()) return CompletableFuture.completedFuture(input)
        val selected = state.frame(frame.view(input))
        val persistent = bindings.filter { it.persistent }.groupBy { it.scope }.mapValues { (_, values) -> values.map { it.key }.toSet() }
        val variables = platform.variables.access(selected, persistent)
        return try {
            variables.prepare().thenCompose {
                RequiredSnapshots.capture(selected.snapshot(bindings, variables), requests, scheduler, state, selected, platform)
            }.whenComplete { _, _ -> state.release(selected) }
        } catch (error: Throwable) { state.release(selected); CompletableFuture.failedFuture(error) }
    }
}
