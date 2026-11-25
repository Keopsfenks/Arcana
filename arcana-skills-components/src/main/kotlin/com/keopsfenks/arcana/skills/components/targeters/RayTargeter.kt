package com.keopsfenks.arcana.skills.components.targeters

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.targets.*
import java.util.concurrent.CompletableFuture

class RayTargeter : SkillTargeter(com.keopsfenks.arcana.skills.config.ComponentArguments.describe("ray", "Select the nearest eligible ray contact; blocks stop the ray", *com.keopsfenks.arcana.skills.platform.targets.TargetSelectionPolicy.OPTIONS, "length", "radius")) {
    override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillTargeter {
        com.keopsfenks.arcana.skills.config.ComponentArguments.keys(arguments, descriptor())
        val length = TargeterSupport.number(arguments, compilation, "length", 10, 0.0, 256.0)
        val radius = TargeterSupport.number(arguments, compilation, "radius", 0, 0.0, 16.0)
        val policy = TargetSelectionPolicy.compile(arguments, compilation, excludeSelf = true)
        return CompiledSkillTargeter { context ->
            val input = context.invocation(); val start = TargetGeometry.center(input); val end = TargetGeometry.offset(start, TargetGeometry.forward(start), length.evaluate(input))
            context.query().collisions(start, end, radius.evaluate(input)).thenCompose { hits ->
                require(hits.size <= 4096) { "Ray query exceeds 4096 contacts" }
                val sorted = hits.sortedWith(compareBy<SkillCollision> { it.fraction() }.thenBy { if (it.actor().isEmpty) 0 else 1 }.thenBy { it.actor().map { actor -> actor.id().toString() }.orElse("") })
                var selected = CompletableFuture.completedFuture(SkillSelection.empty())
                var blocked = false
                for (hit in sorted) selected = selected.thenCompose { previous ->
                    if (blocked || previous.entities().isNotEmpty() || previous.locations().isNotEmpty()) CompletableFuture.completedFuture(previous)
                    else if (hit.actor().isEmpty) { blocked = true; TargeterSupport.locations(context, hit.location(), policy) }
                    else TargeterSupport.actor(context, hit.actor().get(), policy)
                }
                selected.thenApply { if (it.entities().isEmpty() && it.locations().isEmpty() && !blocked) policy.apply(input, emptyList(), listOf(end), context.random()) else it }
            }
        }
    }
}
