package com.keopsfenks.arcana.skills.components.effects.flow

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.platform.execution.ParallelScope
import com.keopsfenks.arcana.skills.platform.targets.TargetSelectionPolicy
import java.util.concurrent.CompletableFuture

class ForEachMechanic : SkillMechanic(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("for_each", "Execute isolated per-target sequences and aggregate their outcomes", "select", "effects", "fail-fast")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val selector = if (!arguments.has("select")) null else {
            val raw = arguments.raw("select")
            val args = arguments.child("select", if (raw is String) mapOf("type" to raw) else raw)
            val name = args.string("type")
            val type = try { compilation.component(SkillComponentType.TARGETER, SkillKey.parse(name, compilation.namespace())) }
            catch (error: IllegalArgumentException) { if (':' in name) throw error else compilation.component(SkillComponentType.TARGETER, SkillKey.parse(name, "arcana")) }
            type.compile(args, compilation)
        }
        val body = FlowSupport.effects(arguments, compilation)
        val failFast = arguments.bool("fail-fast", false)
        return CompiledSkillMechanic { execution ->
            val input = execution.invocation()
            val query = execution.services().require(SkillTargetQuery::class.java)
            val selection = selector?.select(SkillTargetingContext(input, query, FlowSupport.random(execution))) ?: CompletableFuture.completedFuture(input.selection())
            selection.thenCompose { selected ->
                var inputs = CompletableFuture.completedFuture(emptyList<SkillInvocation>())
                for (actor in selected.entities()) inputs = inputs.thenCompose { previous -> query.snapshot(actor).thenApply { snapshot ->
                    previous + if (snapshot.isPresent) TargetSelectionPolicy.selected(input, snapshot.get()) else input.derive().target(actor).build()
                } }
                inputs.thenCompose { entities ->
                    val invocations = entities + selected.locations().map { TargetSelectionPolicy.selected(input, it) }
                    execution.services().require(ParallelScope::class.java).forEach(invocations, body, failFast).thenApply(ParallelScope::aggregate)
                }
            }
        }
    }
}
