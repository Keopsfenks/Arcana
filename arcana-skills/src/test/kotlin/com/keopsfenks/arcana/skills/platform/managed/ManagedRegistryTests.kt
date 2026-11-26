package com.keopsfenks.arcana.skills.platform.managed
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.execution.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

class ManagedRegistryTests {
    @TempDir lateinit var directory: Path
    @Test fun actorCapAndConcurrentLateCleanupAreExactlyOnce() {
        val f = PlatformFixture(directory)
        val owner = f.register("native_mark") { _, _ -> CompiledSkillMechanic { f.success() } }
        val registry = f.platform.managed; val actor = f.invocation().actor()
        val leases = (1..64).map { registry.reserve(owner, actor) }
        assertThrows(PlatformBudgetException::class.java) { registry.reserve(owner, actor) }
        val pool = Executors.newFixedThreadPool(4); val closed = AtomicInteger()
        try {
            val callbacks = leases.flatMap { lease -> listOf(CompletableFuture.runAsync({ lease.onClose { closed.incrementAndGet() } }, pool), CompletableFuture.runAsync({ lease.close() }, pool)) }
            CompletableFuture.allOf(*callbacks.toTypedArray()).join()
            assertEquals(64, closed.get()); assertEquals(0, registry.size)
            leases.forEach { it.close() }; assertEquals(64, closed.get())
            owner.close(); assertThrows(PlatformDependencyException::class.java) { registry.reserve(owner, actor) }
        } finally { pool.shutdownNow() }
    }
}
