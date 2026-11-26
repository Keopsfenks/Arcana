package com.keopsfenks.arcana.skills.platform.events
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.execution.PlatformFixture
import io.mockk.*
import org.bukkit.plugin.Plugin
import org.bukkit.event.entity.EntityDamageEvent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

class ReviewInterceptionTests {
    private fun fixture(dir: Path): PlatformFixture = PlatformFixture(dir).also { f ->
        f.register("native_mark") { args, _ -> val name = args.string("name", "held"); CompiledSkillMechanic { context -> context.delay(3).thenApply { f.legacy.marks.add(name); SkillResult.success() } } }
        interceptor(f, "noop") { SkillResult.success() }
    }
    private fun interceptor(f: PlatformFixture, id: String, action: () -> SkillResult) = f.platform.register(f.legacy.plugin, SkillComponentType.INTERCEPTOR, SkillKey("arcana", id), object : SkillInterceptor(SkillDescriptor.of(id, "test")) {
        override fun compile(arguments: SkillArguments, compilation: SkillCompilation) = CompiledSkillInterceptor { _, _, _ -> action() }
    })
    private fun event() = mockk<EntityDamageEvent>().also { every { it.isCancelled } returns false; every { it.damage } returns 2.0; every { it.finalDamage } returns 2.0 }
    private fun provider(f: PlatformFixture, supplied: () -> List<SkillHolder>): Plugin {
        val owner = mockk<Plugin>(); every { owner.name } returns "Alpha"
        f.platform.register(owner, SkillComponentType.HOLDER, SkillKey("alpha", "slots"), object : SkillHolderProvider {
            override fun descriptor() = SkillDescriptor.of("slots", "test")
            override fun holders(actor: SkillActor) = CompletableFuture.completedFuture(supplied())
        }); return owner
    }
    private fun held() = listOf(SkillHolder(SkillKey("alpha", "ring"), "main", listOf(SkillKey("arcana", "held")), emptyMap()))
    @Test fun holderProviderUnloadCancelsAlreadyAdmittedContinuationButNotUnrelatedRoot(@TempDir dir: Path) {
        val f = fixture(dir)
        f.write(id = "held", options = "activation: holder\ntriggers: [attack]\ninterceptors: [{type: noop}]")
        f.write(id = "other", effects = "  - type: native_mark\n    name: other"); f.load()
        val owner = provider(f, ::held); f.platform.refreshHolders(f.invocation().actor()).toCompletableFuture().join()
        val unrelated = f.cast("other"); val event = event(); val listener = f.platform.interceptorListener
        f.scheduler.on(f.legacy.caster) { listener.intercept(SkillKey("arcana", "attack"), f.invocation(), event) }.future.join()
        assertEquals(1, listener.pendingCount); f.engine.closeOwner(owner); listener.observe(event)
        f.scheduler.advance(1); f.scheduler.advance(3)
        assertEquals(listOf("other"), f.legacy.marks); assertTrue(unrelated.join().isSuccess); f.engine.shutdown()
    }
    @Test fun removingAnEqualReequipBindingCannotReviveItsAlreadyRunningNormalRoot(@TempDir dir: Path) {
        val f = fixture(dir); f.write(id = "held", options = "activation: holder\ntriggers: [attack]"); f.write(id = "other", effects = "  - type: native_mark\n    name: other"); f.load()
        var equipped = true; provider(f) { if (equipped) held() else emptyList() }
        val actor = f.invocation().actor(); f.platform.refreshHolders(actor).toCompletableFuture().join()
        val unrelated = f.cast("other"); val running = f.platform.trigger(SkillKey("arcana", "attack"), f.invocation()).toCompletableFuture()
        equipped = false; f.platform.refreshHolders(actor).toCompletableFuture().join()
        equipped = true; f.platform.refreshHolders(actor).toCompletableFuture().join()
        f.scheduler.advance(3); assertEquals(listOf("other"), f.legacy.marks)
        assertEquals(SkillResult.Status.CANCELLED, running.join().single().status()); assertTrue(unrelated.join().isSuccess); f.engine.shutdown()
    }
    @Test fun interceptionBatchCannotReselectAReplacementRevisionAfterAnEarlierInterceptorReloads(@TempDir dir: Path) {
        val f = fixture(dir); val applied = mutableListOf<String>()
        interceptor(f, "old") { applied.add("old"); SkillResult.success() }
        interceptor(f, "new") { applied.add("replacement"); SkillResult.success() }
        interceptor(f, "reload_second") {
            applied.add("reload"); f.write(id = "second", options = "triggers: [attack]\ninterceptors: [{type: new}]"); assertTrue(f.engine.reload().success()); SkillResult.success()
        }
        f.write(id = "first", options = "triggers: [attack]\ninterceptors: [{type: reload_second}]"); f.write(id = "second", options = "triggers: [attack]\ninterceptors: [{type: old}]"); f.load()
        f.scheduler.on(f.legacy.caster) { f.platform.interceptorListener.intercept(SkillKey("arcana", "attack"), f.invocation(), event()) }.future.join()
        assertEquals(listOf("reload"), applied); f.engine.shutdown()
    }
}
