package com.keopsfenks.arcana.skills.platform.values

import com.keopsfenks.arcana.core.math.ExpressionCompilationException
import com.keopsfenks.arcana.core.math.ExpressionEnvironment
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.compile.PlatformExpressionCompiler
import io.mockk.every
import io.mockk.mockk
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class BindingIntegrationTests {
    @Test fun expressionErrorsKeepTheirRealPublicExceptionPositionAndField() {
        val environment = mockk<ExpressionEnvironment>()
        every { environment.compileOrThrow("damage +") } throws ExpressionCompilationException("operand expected", "damage +", 8)
        val compiler = PlatformExpressionCompiler(environment, null, mapOf("damage" to { _: SkillInvocation -> 0.0 }))
        val error = assertThrows(IllegalArgumentException::class.java) { compiler.number("damage +", "bolt.effects[0].amount") }
        assertTrue(error.message!!.contains("bolt.effects[0].amount"))
        assertTrue(error.message!!.contains("8"))
    }

    @Test fun declaredBindingsReceiveTypedValuesAndUnknownNamesFailCompilation() {
        val environment = mockk<ExpressionEnvironment>()
        every { environment.compileOrThrow(any()) } throws ExpressionCompilationException("unknown variable", "param_missing", 0)
        val compiler = PlatformExpressionCompiler(environment, null, emptyMap())
        assertThrows(IllegalArgumentException::class.java) { compiler.number("param_missing", "amount") }
        assertEquals(4.0, compiler.number(4, "amount").evaluate(mockk()))
        assertThrows(IllegalArgumentException::class.java) { compiler.number(Double.NaN, "amount") }
    }
}
