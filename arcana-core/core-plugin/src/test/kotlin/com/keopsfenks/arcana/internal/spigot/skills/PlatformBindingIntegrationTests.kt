package com.keopsfenks.arcana.internal.spigot.skills

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.internal.spigot.math.api.ArcanaExpressionEnvironmentBuilder
import com.keopsfenks.arcana.skills.DefaultSkillEngine
import com.keopsfenks.arcana.skills.compile.SkillCompilerImpl
import com.keopsfenks.arcana.skills.platform.DefaultSkillPlatform
import com.keopsfenks.arcana.skills.platform.values.ParameterBindings
import io.mockk.every
import io.mockk.mockk
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.CompletableFuture

class PlatformBindingIntegrationTests {
    @TempDir lateinit var directory: Path

    @Test fun nativeParametersCompileAndEvaluateWithTheActualExpressionEngine() {
        val plugin = mockk<ArcanaPlugin>(relaxed = true)
        every { plugin.name } returns "Arcana"
        val base = ArcanaExpressionEnvironmentBuilder().variables(*SkillCompilerImpl.VARIABLES).build()
        val engine = DefaultSkillEngine(plugin, directory, base, expressionEnvironmentFactory = { names ->
            ArcanaExpressionEnvironmentBuilder().variables(*names).build()
        })
        var evaluated = 0.0
        engine.platform().register(plugin, SkillComponentType.MECHANIC, SkillKey("arcana", "capture"),
            object : SkillMechanic(SkillDescriptor.of("capture", "Evaluate typed parameter")) {
                override fun compile(arguments: SkillArguments, compilation: SkillCompilation): CompiledSkillMechanic {
                    val amount = compilation.number(arguments.raw("amount"), arguments.path + ".amount")
                    return CompiledSkillMechanic { execution ->
                        evaluated = amount.evaluate(execution.invocation())
                        CompletableFuture.completedFuture(SkillResult.success())
                    }
                }
            })
        Files.writeString(directory.resolve("bolt.yml"), "schema: 2\nid: bolt\nparameters:\n  power: {type: number, default: 6}\neffects:\n  - type: capture\n    amount: 'damage + param_power'\n")
        val result = engine.reload()
        assertTrue(result.success(), result.errors().toString())
        val skill = requireNotNull((engine.platform() as DefaultSkillPlatform).repository.revision(SkillKey("arcana", "bolt"))?.native)
        val player = mockk<Player>()
        every { player.uniqueId } returns UUID.randomUUID()
        val invocation = SkillInvocation.builder(SkillActor.of(player)).data(mapOf("damage" to 8))
            .parameters(ParameterBindings.bind(skill.parameters, emptyMap())).build()
        val execution = mockk<SkillExecutionContext>()
        every { execution.invocation() } returns invocation
        assertTrue(skill.effects.single().execute(execution).toCompletableFuture().join().isSuccess)
        assertEquals(14.0, evaluated)
        engine.shutdown()
    }
}
