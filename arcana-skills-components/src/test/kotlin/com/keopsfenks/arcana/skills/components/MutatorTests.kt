package com.keopsfenks.arcana.skills.components

import com.keopsfenks.arcana.core.skills.SkillArguments
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.mutators.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MutatorTests {
    private fun mutate(type: SkillMutator, fixture: TargeterFixture, values: Map<String, Any> = emptyMap()) = type.compile(SkillArguments(values, "mutator"), fixture.compiler).mutate(fixture.invocation)
    @Test fun roleChangesCopyTheChildAndLeaveParentAndSiblingUntouched() {
        val f = TargeterFixture()
        val parent = f.invocation
        val swapped = mutate(SwapActorTargetMutator(), f)
        assertEquals(f.victim.actor(), swapped.actor())
        assertEquals(f.self.actor(), swapped.target().orElseThrow())
        assertEquals(f.victim.actor(), mutate(TargetAsActorMutator(), f).actor())
        assertEquals(f.trigger.actor(), mutate(TriggerAsTargetMutator(), f).target().orElseThrow())
        assertEquals(f.self.actor(), parent.actor())
        assertEquals(f.victim.actor(), parent.target().orElseThrow())
        assertEquals(parent, f.invocation)
    }
    @Test fun locationChangesPreserveOriginUnlessExplicitAndRotateInDegrees() {
        val f = TargeterFixture()
        f.invocation = f.invocation.derive().location(SkillLocation(f.world, 1.0, 1.2, 0.0)).build()
        val offset = mutate(OffsetLocationMutator(), f, mapOf("x" to 2, "y" to 1, "z" to 3))
        assertEquals(SkillLocation(f.world, 3.0, 2.2, 3.0), offset.location().orElseThrow())
        assertEquals(f.origin, offset.origin().orElseThrow())
        val origin = mutate(OffsetLocationMutator(), f, mapOf("field" to "origin", "x" to 2))
        assertEquals(2.0, origin.origin().orElseThrow().x())
        assertEquals(0.0, f.invocation.origin().orElseThrow().x())
        val rotated = mutate(RotateLocationMutator(), f, mapOf("angle" to 90)).location().orElseThrow()
        assertEquals(0.0, rotated.x(), 1e-9); assertEquals(1.0, rotated.z(), 1e-9)
        assertEquals(SkillLocation(f.world, 1.5, 1.0, .5), mutate(CenterLocationMutator(), f).location().orElseThrow())
    }
    @Test fun typedDataChangesStayLiteralAndNumericOperationsAreFiniteAndIsolated() {
        val f = TargeterFixture()
        f.invocation = f.invocation.derive().data(mapOf("damage" to 4)).build()
        val literal = mutate(SetValueMutator(), f, mapOf("key" to "text", "value" to "param_power + 100"))
        assertEquals("param_power + 100", literal.data().getValue("text").text())
        assertEquals(8.0, mutate(MultiplyValueMutator(), f, mapOf("key" to "damage", "factor" to 2)).data().getValue("damage").number())
        assertEquals(3.0, mutate(ClampValueMutator(), f, mapOf("key" to "damage", "min" to 1, "max" to 3)).data().getValue("damage").number())
        assertEquals(4.0, f.invocation.data().getValue("damage").number())
        assertThrows(IllegalArgumentException::class.java) { mutate(ClampValueMutator(), f, mapOf("key" to "damage", "min" to 5, "max" to 1)) }
        assertThrows(IllegalArgumentException::class.java) { mutate(SetValueMutator(), f, mapOf("value" to 1)) }
        assertThrows(IllegalArgumentException::class.java) { mutate(OffsetLocationMutator(), f, mapOf("typo" to 1)) }
    }
}
