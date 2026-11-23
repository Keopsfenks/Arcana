package com.keopsfenks.arcana.skills.platform.values

import com.keopsfenks.arcana.core.skills.platform.*
import io.mockk.every
import io.mockk.mockk
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.UUID

class ParameterBindingTests {
    @Test fun requiredDefaultsTypesAndUnknownInputsAreChecked() {
        val power = SkillParameter("power", SkillValue.Kind.NUMBER, false, Optional.of(SkillValue.of(6)))
        val name = SkillParameter("name", SkillValue.Kind.TEXT, true, Optional.empty())
        assertEquals(6.0, ParameterBindings.bind(listOf(power), emptyMap()).getValue("power").number())
        assertThrows(IllegalArgumentException::class.java) { ParameterBindings.bind(listOf(name), emptyMap()) }
        assertThrows(IllegalArgumentException::class.java) { ParameterBindings.bind(listOf(power), mapOf("other" to SkillValue.of(1))) }
        assertThrows(IllegalArgumentException::class.java) { ParameterBindings.bind(listOf(power), mapOf("power" to SkillValue.of("1 + 2"))) }
        val supplied = mutableMapOf("power" to SkillValue.of(9))
        val bound = ParameterBindings.bind(listOf(power), supplied)
        supplied["power"] = SkillValue.of(2)
        assertEquals(9.0, bound.getValue("power").number())
        assertThrows(UnsupportedOperationException::class.java) { (bound as MutableMap).clear() }
    }

    @Test fun childFramesCopyLocalsAndReplaceParametersWithoutMutatingTheirParent() {
        val player = mockk<Player>()
        every { player.uniqueId } returns UUID.randomUUID()
        val invocation = SkillInvocation.builder(SkillActor.of(player)).parameters(mapOf("power" to 6)).build()
        val parent = InvocationFrame(invocation)
        val store = VariableStore()
        val key = SkillKey("arcana", "combo")
        store.access(parent).set(SkillVariableScope.INVOCATION, key, SkillValue.of(1), java.time.Duration.ZERO).toCompletableFuture().join()
        val child = parent.fork(invocation.derive().parameters(mapOf("other" to 9)).build())
        store.access(parent).set(SkillVariableScope.INVOCATION, key, SkillValue.of(2), java.time.Duration.ZERO).toCompletableFuture().join()
        assertEquals(1.0, store.access(child).get(SkillVariableScope.INVOCATION, key).orElseThrow().number())
        assertEquals(setOf("power"), parent.invocation.parameters().keys)
        assertEquals(setOf("other"), child.invocation.parameters().keys)
        assertThrows(UnsupportedOperationException::class.java) { (parent.invocation.parameters() as MutableMap).clear() }
    }
}
