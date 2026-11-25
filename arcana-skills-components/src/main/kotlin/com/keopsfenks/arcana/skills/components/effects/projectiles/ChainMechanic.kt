package com.keopsfenks.arcana.skills.components.effects.projectiles
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.platform.execution.PlatformExecutionContext
import com.keopsfenks.arcana.skills.platform.targets.TargetGeometry
import java.util.*
import java.util.concurrent.*

class ChainMechanic : SkillMechanic(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("chain", "Visit at most 100 distinct nearby actors in stable distance order", *ProjectileArguments.HOOK_KEYS, "radius", "jumps", "hit-caster")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val radius = ProjectileArguments.number(arguments, compilation, "radius", 8, .001, 64.0)
        val jumps = ProjectileArguments.number(arguments, compilation, "jumps", 5, 1.0, 100.0, true)
        val caster = arguments.bool("hit-caster", false); val hooks = ProjectileArguments.Hooks(arguments, compilation)
        return CompiledSkillMechanic { context ->
            if (context !is PlatformExecutionContext) CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.UNSUPPORTED_CONTEXT, "Chain requires Arcana execution"))
            else {
                val input = context.invocation(); val visited = LinkedHashSet<UUID>(); if (!caster) visited.add(input.actor().id())
                val limit = jumps.evaluate(input).toInt(); val distance = radius.evaluate(input); val query = context.services().require(SkillTargetQuery::class.java)
                fun next(center: SkillLocation, preferred: SkillActor?, count: Int): CompletableFuture<SkillResult> {
                    if (count >= limit) return CompletableFuture.completedFuture(SkillResult.success())
                    val candidates = if (preferred != null && preferred.id() !in visited) CompletableFuture.completedFuture(listOf(preferred)) else query.entities(TargetGeometry.radius(center, distance)).toCompletableFuture()
                    return candidates.thenCompose { actors ->
                        require(actors.size <= 4096) { "${arguments.path}: chain exceeds 4096 candidates" }
                        var snapshots = CompletableFuture.completedFuture(emptyList<SkillEntitySnapshot>())
                        for (actor in actors.distinctBy { it.id() }.filter { it.id() !in visited }) snapshots = snapshots.thenCompose { before -> query.snapshot(actor).thenApply { found -> if (found.isPresent && TargetGeometry.distanceSquared(center, found.get().location()) <= distance*distance) before+found.get() else before } }
                        snapshots.thenCompose { available ->
                            val ordered = available.sortedWith(compareBy<SkillEntitySnapshot> { TargetGeometry.distanceSquared(center, it.location()) }.thenBy { it.actor().id().toString() })
                            fun choose(index: Int): CompletableFuture<SkillResult> {
                                if (index >= ordered.size) return CompletableFuture.completedFuture(SkillResult.success())
                                val actual = ordered[index]; visited.add(actual.actor().id())
                                return context.derived(input.derive().target(actual.actor()).location(actual.location()).selection(SkillSelection.empty()).build()).thenCompose { child ->
                                    val value = child.invocation()
                                    if (value.target().isEmpty || !hooks.condition.test(value)) { child.state.release(child.frame); choose(index+1) }
                                    else child.execute(hooks.hit + hooks.tick).thenCompose { result ->
                                        child.state.release(child.frame)
                                        if (result.isSuccess) next(actual.location(), null, count+1) else CompletableFuture.completedFuture(result)
                                    }.whenComplete { _, _ -> child.state.release(child.frame) }.toCompletableFuture()
                                }
                            }
                            choose(0)
                        }
                    }
                }
                context.execute(hooks.start).thenCompose { result -> if (result.isSuccess) next(TargetGeometry.center(input), input.target().orElse(null), 0) else CompletableFuture.completedFuture(result) }
                    .thenCompose { result -> if (result.isSuccess) context.execute(hooks.end) else CompletableFuture.completedFuture(result) }
            }
        }
    }
}
