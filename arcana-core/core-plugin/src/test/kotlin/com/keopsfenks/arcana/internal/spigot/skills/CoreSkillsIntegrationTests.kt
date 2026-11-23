package com.keopsfenks.arcana.internal.spigot.skills

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.internal.spigot.ArcanaImpl
import com.keopsfenks.arcana.internal.spigot.math.api.ArcanaExpressionEnvironmentBuilder
import com.keopsfenks.arcana.skills.BuiltinComponents
import com.keopsfenks.arcana.skills.DefaultSkillEngine
import com.keopsfenks.arcana.skills.compile.SkillCompilerImpl
import com.keopsfenks.arcana.skills.registry.ComponentRegistry
import io.mockk.every
import io.mockk.mockk
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Consumer
import java.util.logging.Logger

class CoreSkillsIntegrationTests {
    @TempDir lateinit var directory: Path

    @Test fun `standalone bridge implements the skill engine entry point`() {
        assertEquals(SkillEngine::class.java, ArcanaImpl::class.java.getDeclaredMethod("getSkillEngine").returnType)
    }

    @Test fun `real expression errors identify the exact YAML field`() {
        val plugin = mockk<ArcanaPlugin>()
        val environment = ArcanaExpressionEnvironmentBuilder().variables(*SkillCompilerImpl.VARIABLES).build()
        val engine = DefaultSkillEngine(plugin, directory, environment)
        BuiltinComponents.install(engine, plugin)
        Files.writeString(directory.resolve("bad.yml"), "id: bad\neffects:\n  - type: heal\n    amount: 'damage +'\n")
        val result = engine.reload()
        assertFalse(result.success())
        assertTrue(result.errors().single().contains("effects[0].amount"), result.errors().toString())
    }

    @Test fun `bundled examples compile with the actual Arcana expression engine`() {
        val plugin = mockk<ArcanaPlugin>()
        every { plugin.logger } returns Logger.getLogger("CoreSkillsTest")
        val environment = ArcanaExpressionEnvironmentBuilder().variables(*SkillCompilerImpl.VARIABLES).build()
        val engine = DefaultSkillEngine(plugin, directory, environment)
        BuiltinComponents.install(engine, plugin)
        for (id in listOf("join", "melee_attack", "sneak")) {
            engine.registerTrigger(plugin, object : SkillTrigger(id) {
                override fun listen(fire: Consumer<SkillContext>) = AutoCloseable {}
            })
        }
        for (name in listOf("_welcome.yml", "_vampiric_strike.yml", "_combo.yml")) {
            val input = requireNotNull(javaClass.getResourceAsStream("/arcana-skills/examples/$name"))
            input.use { Files.copy(it, directory.resolve(name.removePrefix("_"))) }
        }
        val result = engine.reload()
        assertTrue(result.success(), result.errors().toString())
        assertEquals(3, result.loaded())
        assertEquals(3, result.disabled())
        val compiler = SkillCompilerImpl(environment, ComponentRegistry(), ComponentRegistry(), ComponentRegistry())
        val expression = compiler.number("2 + damage * 0.1", "vampiric.amount")
        assertEquals(3.0, expression.evaluate(SkillContext.builder(mockk<Player>()).value("damage", 10.0).build()))
        engine.shutdown()
    }
}
