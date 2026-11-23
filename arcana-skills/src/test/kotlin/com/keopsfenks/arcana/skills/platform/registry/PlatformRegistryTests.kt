package com.keopsfenks.arcana.skills.platform.registry

import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.execution.EngineFixture
import com.keopsfenks.arcana.skills.platform.DefaultSkillPlatform
import io.mockk.every
import io.mockk.mockk
import org.bukkit.plugin.Plugin
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

class PlatformRegistryTests {
    @TempDir lateinit var directory: Path
    private fun owner(name: String) = mockk<Plugin>().also { every { it.name } returns name }
    private fun mechanic() = object : SkillMechanic(SkillDescriptor.of("mark", "Test mechanic")) {
        override fun compile(arguments: SkillArguments, compilation: SkillCompilation) = CompiledSkillMechanic {
            CompletableFuture.completedFuture(SkillResult.success())
        }
    }

    @Test fun namespacesAndTypesDoNotReplaceAnotherOwner() {
        val f = EngineFixture(directory)
        val alpha = owner("Alpha")
        val beta = owner("Beta")
        val platform = f.engine.platform() as DefaultSkillPlatform
        val first = platform.register(alpha, SkillComponentType.MECHANIC, SkillKey("alpha", "mark"), mechanic())
        val second = platform.register(beta, SkillComponentType.MECHANIC, SkillKey("beta", "mark"), mechanic())
        assertThrows(IllegalArgumentException::class.java) {
            platform.register(alpha, SkillComponentType.MECHANIC, SkillKey("beta", "mark"), mechanic())
        }
        assertThrows(IllegalArgumentException::class.java) {
            platform.register(alpha, SkillComponentType.MECHANIC, SkillKey("alpha", "mark"), mechanic())
        }
        assertSame(first, platform.registry(SkillComponentType.MECHANIC).get(SkillKey("alpha", "mark")))
        f.engine.closeOwner(alpha)
        assertFalse(first.isActive)
        assertTrue(second.isActive)
        assertNull(platform.registry(SkillComponentType.MECHANIC).get(SkillKey("alpha", "mark")))
        assertThrows(IllegalStateException::class.java) {
            platform.register(alpha, SkillComponentType.MECHANIC, SkillKey("alpha", "mark"), mechanic())
        }
        f.engine.enableOwner(alpha)
        assertTrue(platform.register(alpha, SkillComponentType.MECHANIC, SkillKey("alpha", "mark"), mechanic()).isActive)
        second.close(); second.close()
        assertFalse(second.isActive)
    }

    @Test fun rawJavaTypeMisuseCannotEnterTheWrongRegistry() {
        val f = EngineFixture(directory)
        @Suppress("UNCHECKED_CAST")
        val wrong = SkillComponentType.PREDICATE as SkillComponentType<SkillMechanic>
        assertThrows(ClassCastException::class.java) {
            f.engine.platform().register(owner("Alpha"), wrong, SkillKey("alpha", "mark"), mechanic())
        }
    }

    @Test fun namespaceNormalizationCannotMergeDifferentOwners() {
        val f = EngineFixture(directory)
        f.engine.platform().register(owner("A-B"), SkillComponentType.MECHANIC, SkillKey("a_b", "mark"), mechanic())
        val other = object : SkillMechanic(SkillDescriptor.of("other", "Test mechanic")) {
            override fun compile(arguments: SkillArguments, compilation: SkillCompilation) = CompiledSkillMechanic {
                CompletableFuture.completedFuture(SkillResult.success())
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            f.engine.platform().register(owner("A_B"), SkillComponentType.MECHANIC, SkillKey("a_b", "other"), other)
        }
    }
}
