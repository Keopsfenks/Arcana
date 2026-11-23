package com.keopsfenks.arcana.skills.registry

import com.keopsfenks.arcana.core.skills.*
import io.mockk.mockk
import org.bukkit.plugin.Plugin
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

class ComponentRegistryTests {
    private fun effect(id: String) = object : SkillEffect(id) {
        override fun compile(arguments: SkillArguments, compiler: SkillCompiler) =
            CompiledSkillEffect { CompletableFuture.completedFuture(SkillResult.success()) }
    }

    @Test fun `duplicate ids reject without replacing their owner`() {
        val registry = ComponentRegistry<SkillEffect>()
        val owner = mockk<Plugin>()
        val first = registry.register(owner, effect("particle"))
        assertThrows(IllegalArgumentException::class.java) { registry.register(mockk(), effect("particle")) }
        assertSame(first, registry.get("particle"))
    }

    @Test fun `closing a handle is idempotent and notifies dependency removal once`() {
        val removed = mutableListOf<String>()
        val registry = ComponentRegistry<SkillEffect> { removed.add(it.id) }
        val handle = registry.register(mockk(), effect("particle"))
        handle.close()
        handle.close()
        assertFalse(handle.isActive)
        assertNull(registry.get("particle"))
        assertEquals(listOf("particle"), removed)
        val replacement = registry.register(mockk(), effect("particle"))
        handle.close()
        assertSame(replacement, registry.get("particle"))
    }

    @Test fun `closing an owner only removes that owners components`() {
        val registry = ComponentRegistry<SkillEffect>()
        val owner = mockk<Plugin>()
        val first = registry.register(owner, effect("particle"))
        val second = registry.register(owner, effect("heal"))
        val other = registry.register(mockk(), effect("damage"))
        registry.closeOwner(owner)
        assertFalse(first.isActive)
        assertFalse(second.isActive)
        assertTrue(other.isActive)
        assertEquals(listOf(other), registry.snapshot())
    }
}
