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
        return context.query().snapshot(actor).thenApply { captured -> policy.apply(context.invocation(), captured.map { listOf(it) }.orElse(emptyList()), emptyList(), context.random()) }.toCompletableFuture()
    }
    fun locations(context: SkillTargetingContext, location: SkillLocation, policy: TargetSelectionPolicy) = CompletableFuture.completedFuture(policy.apply(context.invocation(), emptyList(), listOf(location), context.random()))
    fun area(context: SkillTargetingContext, bounds: SkillBounds, policy: TargetSelectionPolicy, matches: (SkillEntitySnapshot) -> Boolean): CompletableFuture<SkillSelection> =
        context.query().entities(bounds).thenCompose { candidates ->
            var snapshots = CompletableFuture.completedFuture(emptyList<SkillEntitySnapshot>())
            require(candidates.size <= 4096) { "Target query exceeds 4096 candidates" }
            for (candidate in candidates.distinctBy { it.id() }) snapshots = snapshots.thenCompose { previous ->
                context.query().snapshot(candidate).thenApply { optional -> if (optional.isPresent && matches(optional.get())) previous + optional.get() else previous }
            }
            snapshots.thenApply { policy.apply(context.invocation(), it, emptyList(), context.random()) }
        }.toCompletableFuture()
}
