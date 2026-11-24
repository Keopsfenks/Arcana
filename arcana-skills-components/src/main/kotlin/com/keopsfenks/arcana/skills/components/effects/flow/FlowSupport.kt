package com.keopsfenks.arcana.skills.components.effects.flow

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.execution.*
import com.keopsfenks.arcana.skills.platform.values.ParameterBindings
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.function.DoubleSupplier
import java.util.concurrent.CompletionStage

internal object FlowSupport {
    fun success() = CompletableFuture.completedFuture(SkillResult.success())
    fun unsupported() = CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.UNSUPPORTED_CONTEXT, "Flow requires an Arcana execution context"))
    fun effects(args: SkillArguments, compiler: SkillCompilation, key: String = "effects", optional: Boolean = false): List<CompiledSkillMechanic> {
        val raw = if (optional) args.list(key, emptyList<Any>()) else args.list(key)
        require(optional || raw.isNotEmpty()) { "${args.path}.$key: effects must not be empty" }
        return compiler.effects(raw, "${args.path}.$key")
    }
    fun choices(args: SkillArguments): List<SkillArguments> = args.list("choices").also { require(it.isNotEmpty() && it.size <= 1024) { "${args.path}.choices: expected 1..1024 choices" } }
        .mapIndexed { index, raw -> args.child("choices[$index]", raw) }
    fun random(execution: SkillExecutionContext): DoubleSupplier = execution.services().require(DoubleSupplier::class.java)
    fun roll(execution: SkillExecutionContext) = random(execution).asDouble.also { require(it.isFinite() && it >= 0 && it < 1) { "Random source must return a finite value in [0,1)" } }
    fun cursor(context: PlatformExecutionContext, node: UUID): FlowCursorKey {
        context.state.checkActive()
        return FlowCursorKey(context.invocation().actor().id(), context.definition.key, node, requireNotNull(context.state.snapshot.entries[context.definition.key]))
    }
    fun parameter(raw: Any?, compiler: SkillCompilation, path: String): (SkillInvocation) -> SkillValue {
        if (raw is Map<*, *> && raw.keys == setOf("expression")) {
            val expression = compiler.number(raw["expression"], "$path.expression")
            return { SkillValue.of(expression.evaluate(it)) }
        }
        val value = if (raw is Map<*, *> && raw.keys == setOf("type", "value")) {
            ParameterBindings.literal(ParameterBindings.kind(raw["type"] as? String ?: throw IllegalArgumentException("$path.type: expected a value type"), "$path.type"), raw["value"], compiler.namespace(), path)
        } else SkillValue.of(raw)
        return { value }
    }
    fun evaluate(context: PlatformExecutionContext, predicate: SkillPredicate, action: (Boolean, PlatformExecutionContext) -> CompletionStage<SkillResult>): CompletableFuture<SkillResult> = context.derived(context.invocation()).thenCompose { child ->
        child.track(child.engine.scheduler.on(child.invocation().actor().entity()) {
            child.state.checkActive(); child.frame.checkActive(); predicate.test(child.invocation())
        }).thenCompose { action(it, child).toCompletableFuture() }.whenComplete { _, _ -> child.state.release(child.frame) }
    }
}
