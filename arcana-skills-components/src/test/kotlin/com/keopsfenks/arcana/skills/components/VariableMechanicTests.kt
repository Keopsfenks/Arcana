package com.keopsfenks.arcana.skills.components

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.effects.variables.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

class VariableMechanicTests {
    @Test fun eachVariableOperationHasItsOwnFactoryAndUsesTypedServices() {
        val variables = mockk<SkillVariables>(relaxed = true)
        val execution = mockk<SkillExecutionContext>()
        every { execution.invocation() } returns mockk<SkillInvocation>()
        every { execution.services() } returns SkillServices.of(mapOf(SkillVariables::class.java to variables))
        val compiler = mockk<SkillCompilation>()
        every { compiler.namespace() } returns "arcana"
        every { compiler.number(2, any()) } returns SkillNumericExpression { 2.0 }
        every { variables.set(any(), any(), any(), any()) } returns CompletableFuture.completedFuture(null)
        every { variables.add(any(), any(), any()) } returns CompletableFuture.completedFuture(4.0)
        every { variables.remove(any(), any()) } returns CompletableFuture.completedFuture(true)
        val set = SetVariableMechanic().compile(SkillArguments(mapOf("type" to "set_variable", "key" to "combo", "value" to 2), "set"), compiler)
        val add = AddVariableMechanic().compile(SkillArguments(mapOf("type" to "add_variable", "key" to "combo", "amount" to 2), "add"), compiler)
        val remove = RemoveVariableMechanic().compile(SkillArguments(mapOf("type" to "remove_variable", "key" to "combo"), "remove"), compiler)
        assertTrue(set.execute(execution).toCompletableFuture().join().isSuccess)
        assertTrue(add.execute(execution).toCompletableFuture().join().isSuccess)
        assertTrue(remove.execute(execution).toCompletableFuture().join().isSuccess)
        verify { variables.set(SkillVariableScope.INVOCATION, SkillKey("arcana", "combo"), SkillValue.of(2), any()) }
        verify { variables.add(SkillVariableScope.INVOCATION, SkillKey("arcana", "combo"), 2.0) }
        verify { variables.remove(SkillVariableScope.INVOCATION, SkillKey("arcana", "combo")) }
    }
}
