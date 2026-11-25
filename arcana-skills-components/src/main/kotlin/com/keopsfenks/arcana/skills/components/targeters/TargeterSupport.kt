package com.keopsfenks.arcana.skills.components.targeters

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.config.ComponentArguments
import com.keopsfenks.arcana.skills.platform.targets.*
import java.util.concurrent.CompletableFuture

internal object TargeterSupport {
    fun keys(args: SkillArguments, vararg extra: String) = ComponentArguments.keys(args, *TargetSelectionPolicy.OPTIONS, *extra)
    fun number(args: SkillArguments, compiler: SkillCompilation, name: String, fallback: Number, min: Double, max: Double): SkillNumericExpression {
        fun validate(number: Double): Double = number.also { require(it.isFinite() && it in min..max) { "${args.path}.$name: expected a finite number in $min..$max" } }
        val raw = args.raw(name, fallback)
        if (raw is Number) validate(raw.toDouble())
        val compiled = compiler.number(raw, "${args.path}.$name")
        return SkillNumericExpression { validate(compiled.evaluate(it)) }
    }
    fun actor(context: SkillTargetingContext, actor: SkillActor?, policy: TargetSelectionPolicy): CompletableFuture<SkillSelection> {
        if (actor == null) return CompletableFuture.completedFuture(SkillSelection.empty())
        return context.query().snapshot(actor).thenCompose { captured ->
            if (captured.isEmpty || !policy.eligible(context.invocation(), captured.get())) CompletableFuture.completedFuture(SkillSelection.empty())
            else context.query().project(context.invocation(), captured.get()).thenApply { projected ->
                policy.apply(context.invocation(), listOf(captured.get()), emptyList(), context.random(), projected.map { mapOf(actor.id() to it) }.orElse(emptyMap()))
            }
        }.toCompletableFuture()
    }
    fun locations(context: SkillTargetingContext, location: SkillLocation, policy: TargetSelectionPolicy) = context.query().projectLocation(context.invocation(), location).thenApply { projected ->
        policy.apply(context.invocation(), emptyList(), listOf(location), context.random(), locationInputs = projected.map { mapOf(location to it) }.orElse(emptyMap()))
    }.toCompletableFuture()
    fun area(context: SkillTargetingContext, bounds: SkillBounds, policy: TargetSelectionPolicy, matches: (SkillEntitySnapshot) -> Boolean): CompletableFuture<SkillSelection> =
        context.query().entities(bounds).thenCompose { candidates ->
            var snapshots = CompletableFuture.completedFuture(emptyList<SkillEntitySnapshot>())
            require(candidates.size <= 4096) { "Target query exceeds 4096 candidates" }
            for (candidate in candidates.distinctBy { it.id() }) snapshots = snapshots.thenCompose { previous ->
                context.query().snapshot(candidate).thenApply { optional -> if (optional.isPresent && matches(optional.get()) && policy.eligible(context.invocation(), optional.get())) previous + optional.get() else previous }
            }
            snapshots.thenCompose { candidates ->
                val projected = linkedMapOf<java.util.UUID, SkillInvocation>(); var result = CompletableFuture.completedFuture(Unit)
                for (candidate in candidates) result = result.thenCompose {
                    context.query().project(context.invocation(), candidate).thenApply { input -> input.ifPresent { projected[candidate.actor().id()] = it }; Unit }
                }
                result.thenApply { policy.apply(context.invocation(), candidates, emptyList(), context.random(), projected) }
            }
        }.toCompletableFuture()
}
