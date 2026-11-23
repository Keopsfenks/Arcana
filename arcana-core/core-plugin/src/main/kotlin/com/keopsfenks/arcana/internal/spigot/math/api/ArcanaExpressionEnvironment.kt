package com.keopsfenks.arcana.internal.spigot.math.api

import com.keopsfenks.arcana.core.math.CompiledExpression
import com.keopsfenks.arcana.core.math.ExpressionEnvironment
import com.keopsfenks.arcana.internal.spigot.math.ExpressionParser
import com.keopsfenks.arcana.internal.spigot.math.functional.ExpressionEnv
import com.keopsfenks.arcana.core.math.ExpressionCompilationException as PublicCompilationException

/**
 * Adapts the internal [ExpressionEnv] to the public [ExpressionEnvironment] contract.
 *
 * The wrapped environment is never mutated after construction, so this is safe to share across
 * threads: [ExpressionParser] holds all per-compilation state.
 */
class ArcanaExpressionEnvironment(
    private val env: ExpressionEnv
) : ExpressionEnvironment {

    override fun compile(expression: String): CompiledExpression? =
        try {
            ArcanaCompiledExpression(ExpressionParser(expression, env).parse())
        } catch (e: Exception) {
            null
        }

    override fun compileOrThrow(expression: String): CompiledExpression {
        val parser = ExpressionParser(expression, env)
        try {
            return ArcanaCompiledExpression(parser.parse())
        } catch (e: Exception) {
            throw PublicCompilationException(
                e.message ?: "Failed to compile expression",
                expression,
                parser.cursor
            )
        }
    }
}
