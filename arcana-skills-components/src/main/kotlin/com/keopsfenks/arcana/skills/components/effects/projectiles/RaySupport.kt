package com.keopsfenks.arcana.skills.components.effects.projectiles

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.execution.*
import com.keopsfenks.arcana.skills.platform.projectiles.*
import com.keopsfenks.arcana.skills.platform.targets.TargetGeometry
import java.util.concurrent.*

internal class RaySupport(args: SkillArguments, compiler: SkillCompilation) {
    val hooks = ProjectileArguments.Hooks(args, compiler)
    private val length = ProjectileArguments.number(args, compiler, "length", 10, .001, 256.0)
    private val radius = ProjectileArguments.number(args, compiler, "radius", .1, 0.0, 16.0)
    private val direction = ProjectileArguments.direction(args, compiler)
    private val pierce = ProjectileArguments.number(args, compiler, "pierce", 0, 0.0, 255.0, true)
    val maximum = ProjectileArguments.number(args, compiler, "max-hits", 256, 1.0, 256.0, true)
    private val hitCaster = args.bool("hit-caster", false)
    fun fire(context: PlatformExecutionContext, ledger: ProjectileHitLedger, tick: Long): CompletableFuture<SkillResult> {
        val input = context.invocation(); val start = TargetGeometry.center(input)
        val end = TargetGeometry.offset(start, direction(input), length.evaluate(input)); val width = radius.evaluate(input)
        val query = context.services().require(SkillTargetQuery::class.java)
        return query.loaded(CollisionResolver.bounds(start, end, width)).thenCompose { loaded ->
            if (!loaded) CompletableFuture.completedFuture(emptyList<SkillCollision>()) else query.collisions(start, end, width)
        }.thenCompose { hits ->
            val ordered = CollisionResolver.ordered(hits, input.actor(), hitCaster)
            var result = CompletableFuture.completedFuture(SkillResult.success()); var blocked = false; var accepted = 0
            val allowed = pierce.evaluate(input).toInt()+1
            for (hit in ordered) result = result.thenCompose { prior ->
                if (!prior.isSuccess || blocked || accepted >= allowed || ledger.exhausted()) CompletableFuture.completedFuture(prior)
                else {
                    if (hit.actor().isEmpty) blocked = true
                    val actual = hit.actor().orElse(null)
                    val changed = input.derive().target(actual).location(hit.location()).selection(SkillSelection.empty()).build()
                    context.derived(changed).thenCompose { child ->
                        val value = child.invocation()
                        if (actual != null && value.target().orElse(null) != actual || !hooks.condition.test(value) || !ledger.accept(actual?.id(), tick)) CompletableFuture.completedFuture(SkillResult.success())
                        else { accepted++; child.execute(hooks.hit).toCompletableFuture() }
                    .whenComplete { _, _ -> child.state.release(child.frame) } }
                }
            }
            result.thenCompose { value -> if (value.isSuccess) context.execute(hooks.tick) else CompletableFuture.completedFuture(value) }
        }.toCompletableFuture()
    }
    companion object { val KEYS = arrayOf("length", "radius", "direction", "pierce", "max-hits", "hit-caster") }
}
