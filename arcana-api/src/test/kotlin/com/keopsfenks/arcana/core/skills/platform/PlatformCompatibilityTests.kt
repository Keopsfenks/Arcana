package com.keopsfenks.arcana.core.skills.platform

import com.keopsfenks.arcana.core.skills.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.lang.invoke.MethodType
import java.util.concurrent.CompletableFuture

class PlatformCompatibilityTests {
    @Test fun legacyDescriptorsAndConsumerRemainUnchanged() {
        val resource = requireNotNull(javaClass.getResourceAsStream("/skills-v1-descriptors.txt"))
        val entries = resource.bufferedReader().use { it.readLines() }
        assertTrue(entries.size > 60)
        for (entry in entries) {
            val (name, member, signature) = entry.split('|')
            val type = Class.forName(name)
            val present = if (member == "<init>") type.declaredConstructors.any {
                MethodType.methodType(Void.TYPE, it.parameterTypes.toList()).toMethodDescriptorString() == signature
            } else type.declaredMethods.any {
                it.name == member && MethodType.methodType(it.returnType, it.parameterTypes.toList()).toMethodDescriptorString() == signature
            }
            assertTrue(present, "Removed legacy descriptor: $entry")
        }
        val engine = mockk<SkillEngine>()
        val player = mockk<Player>()
        every { engine.cast("legacy", any()) } returns CompletableFuture.completedFuture(SkillResult.success())
        assertTrue(LegacyConsumer.cast(engine, player).toCompletableFuture().join().isSuccess)
        verify { engine.cast("legacy", match { it.caster === player && it.getNumber("damage") == 2.0 }) }
        val execution = mockk<SkillExecution>()
        every { execution.onTarget(SkillTarget.CASTER, any()) } returns CompletableFuture.completedFuture(SkillResult.success())
        val effect = LegacyConsumer().compile(SkillArguments(emptyMap<String, Any>(), "legacy"), mockk())
        assertTrue(effect.execute(execution).toCompletableFuture().join().isSuccess)
        verify { execution.onTarget(SkillTarget.CASTER, any()) }
    }

    @Test fun oldEngineImplementationsHaveAnExplicitUnsupportedPlatformDefault() {
        val engine = mockk<SkillEngine>()
        every { engine.platform() } answers { callOriginal() }
        assertThrows(UnsupportedOperationException::class.java) { engine.platform() }
    }
}
