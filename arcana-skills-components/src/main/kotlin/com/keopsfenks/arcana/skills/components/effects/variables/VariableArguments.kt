package com.keopsfenks.arcana.skills.components.effects.variables

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.values.VariableTargetException
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.CompletionStage
import java.util.concurrent.ExecutionException

internal object VariableArguments {
    fun scope(args: SkillArguments): SkillVariableScope = try {
        SkillVariableScope.valueOf(args.string("scope", "invocation").uppercase(Locale.ROOT))
    } catch (_: IllegalArgumentException) { throw args.error("scope", "unknown variable scope") }

    fun run(execution: SkillExecutionContext, action: (SkillVariables) -> CompletionStage<*>): CompletionStage<SkillResult> {
        return try {
            action(execution.services().require(SkillVariables::class.java)).handle { _, error ->
                if (error == null) SkillResult.success()
                else {
                    var cause = error
                    while ((cause is CompletionException || cause is ExecutionException) && cause.cause != null) cause = cause.cause!!
                    if (cause is VariableTargetException) SkillResult.of(SkillResult.Status.NO_TARGET, cause.message ?: "Variable target unavailable")
                    else throw CompletionException(cause)
                }
            }
        } catch (error: VariableTargetException) { CompletableFuture.completedFuture(SkillResult.of(SkillResult.Status.NO_TARGET, error.message ?: "Variable target unavailable")) }
    }
}
