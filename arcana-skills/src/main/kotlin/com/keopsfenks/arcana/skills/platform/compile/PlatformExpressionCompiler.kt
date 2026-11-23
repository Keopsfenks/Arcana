package com.keopsfenks.arcana.skills.platform.compile

import com.keopsfenks.arcana.core.math.ExpressionCompilationException
import com.keopsfenks.arcana.core.math.ExpressionEnvironment
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.compile.SkillCompilerImpl

/** Typed binding arrays, not expression text substitution. Unused bindings are never read. */
class PlatformExpressionCompiler(
    private val environment: ExpressionEnvironment,
    private val environmentFactory: ((Array<String>) -> ExpressionEnvironment)?,
    private val bindings: Map<String, (SkillInvocation) -> Double>
) {
    fun number(value: Any?, path: String): SkillNumericExpression {
        if (value is Number) {
            val number = value.toDouble()
            require(number.isFinite()) { "$path: number must be finite" }
            return SkillNumericExpression { number }
        }
        require(value is String && value.isNotBlank() && value.length <= 4096) { "$path: expected a number or expression" }
        val identifiers = Regex("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b").findAll(value).map { it.value }.toSet()
        val used = bindings.keys.filter { it in identifiers }.toSet()
        val names: Array<String>
        val compiler: ExpressionEnvironment
        if (environmentFactory != null) {
            names = used.sorted().toTypedArray()
            compiler = environmentFactory.invoke(names)
        } else {
            require(used.all { it in SkillCompilerImpl.VARIABLES }) { "$path: parameter/variable expressions require an environment factory" }
            names = SkillCompilerImpl.VARIABLES
            compiler = environment
        }
        val expression = try { compiler.compileOrThrow(value) }
        catch (error: ExpressionCompilationException) { throw IllegalArgumentException("$path: invalid expression at ${error.position}: ${error.message}", error) }
        catch (error: IllegalArgumentException) { throw IllegalArgumentException("$path: invalid expression: ${error.message}", error) }
        return SkillNumericExpression { invocation ->
            val values = names.map { name ->
                if (name !in used) 0.0 else requireNotNull(bindings[name])(invocation).also { require(it.isFinite()) { "$path: binding $name is not finite" } }
            }.toDoubleArray()
            expression.evaluate(*values).also { require(it.isFinite()) { "$path: expression produced a non-finite result" } }
        }
    }
}
