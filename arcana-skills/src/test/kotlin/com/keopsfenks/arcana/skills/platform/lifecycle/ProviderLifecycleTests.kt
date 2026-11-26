package com.keopsfenks.arcana.skills.platform.lifecycle

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
import java.util.concurrent.*

class ProviderLifecycleTests {
    @TempDir lateinit var directory: Path
    private fun owner(name: String) = mockk<Plugin>().also { every { it.name } returns name }
    private fun mechanic() = object : SkillMechanic(SkillDescriptor.of("mark", "Test mechanic")) {
        override fun compile(arguments: SkillArguments, compilation: SkillCompilation) = CompiledSkillMechanic {
            CompletableFuture.completedFuture(SkillResult.success())
        }
    }

    @Test fun ownerDisableAndShutdownCannotBeOvertakenByMetadataOrRegistration() {
        for (shutdown in listOf(false, true)) {
            val f = EngineFixture(directory)
            val alpha = owner("Alpha")
            val component = mockk<SkillMechanic>()
            val entered = CountDownLatch(1)
            val resume = CountDownLatch(1)
            every { component.descriptor() } answers {
                entered.countDown()
                check(resume.await(3, TimeUnit.SECONDS))
                SkillDescriptor.of("mark", "Delayed metadata")
            }
            val pool = Executors.newFixedThreadPool(2)
            try {
                val registered = pool.submit<SkillRegistration> {
                    f.engine.platform().register(alpha, SkillComponentType.MECHANIC, SkillKey("alpha", "mark"), component)
                }
                assertTrue(entered.await(3, TimeUnit.SECONDS))
                val closure = pool.submit { if (shutdown) f.engine.shutdown() else f.engine.closeOwner(alpha) }
                // Metadata is outside the gate: closure is allowed to finish before releasing it.
                closure.get(2, TimeUnit.SECONDS)
                resume.countDown()
                val error = assertThrows(ExecutionException::class.java) { registered.get(2, TimeUnit.SECONDS) }
                assertTrue(error.cause is IllegalStateException)
                assertTrue((f.engine.platform() as DefaultSkillPlatform).registry(SkillComponentType.MECHANIC).snapshot().isEmpty())
            } finally { resume.countDown(); pool.shutdownNow() }
        }
    }

    @Test fun externalCleanupMayRegisterAnotherOwnerWithoutHoldingTheGate() {
        val f = EngineFixture(directory)
        val alpha = owner("Alpha")
        val beta = owner("Beta")
        val pool = Executors.newSingleThreadExecutor()
        var replacement: SkillRegistration? = null
        val handle = f.engine.registerTrigger(alpha, object : SkillTrigger("custom") {
            override fun listen(fire: java.util.function.Consumer<SkillContext>) = AutoCloseable {
                replacement = pool.submit<SkillRegistration> {
                    f.engine.platform().register(beta, SkillComponentType.MECHANIC, SkillKey("beta", "mark"), mechanic())
                }.get(2, TimeUnit.SECONDS)
            }
        })
        try {
            f.engine.closeOwner(alpha)
            assertFalse(handle.isActive)
            assertTrue(requireNotNull(replacement).isActive)
            assertThrows(IllegalStateException::class.java) {
                f.engine.registerCondition(alpha, object : SkillCondition("denied") {
                    override fun compile(arguments: SkillArguments, compiler: SkillCompiler) = CompiledSkillCondition { true }
                })
            }
        } finally { pool.shutdownNow(); f.engine.shutdown() }
    }
}
