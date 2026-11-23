package com.keopsfenks.arcana.skills.triggers

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.scheduling.*
import com.keopsfenks.arcana.core.skills.SkillContext
import com.keopsfenks.arcana.skills.BuiltinComponents
import com.keopsfenks.arcana.skills.execution.SkillDamageScope
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.Material
import org.bukkit.Server
import org.bukkit.block.Block
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.event.player.PlayerToggleSprintEvent
import org.bukkit.plugin.PluginManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.nio.file.Files
import com.keopsfenks.arcana.skills.DefaultSkillEngine
import com.keopsfenks.arcana.skills.compile.CompilerFixture
import com.keopsfenks.arcana.skills.execution.EngineFixture
import com.keopsfenks.arcana.core.skills.*
import java.util.concurrent.CompletableFuture
import java.util.logging.Logger

class TriggerAdapterTests {
    private class Host(val player: Player = mockk()) {
        val plugin = mockk<ArcanaPlugin>()
        val manager = mockk<PluginManager>(relaxed = true)
        val queued = mutableListOf<Runnable>()
        val handles = mutableListOf<ArcanaTask>()
        init {
            val server = mockk<Server>()
            val scheduler = mockk<Scheduler>()
            val entity = mockk<EntityTaskContext>()
            every { plugin.server } returns server
            every { plugin.logger } returns Logger.getLogger("TriggerTest")
            every { server.pluginManager } returns manager
            every { plugin.scheduler } returns scheduler
            every { scheduler.on(any()) } returns entity
            every { entity.onRetired(any()) } returns entity
            every { entity.run(any<Runnable>()) } answers {
                queued.add(firstArg())
                mockk<ArcanaTask>(relaxed = true).also { handles.add(it) }
            }
        }
        fun flush() { val jobs = queued.toList(); queued.clear(); jobs.forEach { it.run() } }
    }

    @Test fun `queued events cannot select a replacement snapshot and empty trigger indexes schedule nothing`(@TempDir directory: Path) {
        val f = EngineFixture(directory)
        val host = Host(f.caster)
        val engine = DefaultSkillEngine(host.plugin, directory, CompilerFixture().environment, f.scheduler)
        var calls = 0
        engine.registerEffect(host.plugin, object : SkillEffect("mark") {
            override fun compile(arguments: SkillArguments, compiler: SkillCompiler) = CompiledSkillEffect {
                calls++
                CompletableFuture.completedFuture(SkillResult.success())
            }
        })
        val trigger = BlockBreakTrigger(host.plugin)
        engine.registerTrigger(host.plugin, trigger)
        val block = mockk<Block>()
        every { block.type } returns Material.STONE
        val event = mockk<BlockBreakEvent>()
        every { event.isCancelled } returns false
        every { event.player } returns f.caster
        every { event.block } returns block
        f.write(options = "triggers: [block_break]")
        assertTrue(engine.reload().success())
        trigger.onEvent(event)
        f.write(options = "triggers: [block_break]")
        assertTrue(engine.reload().success())
        host.flush()
        assertEquals(0, calls)
        Files.delete(directory.resolve("one.yml"))
        assertTrue(engine.reload().success())
        trigger.onEvent(event)
        assertTrue(host.queued.isEmpty())
        engine.shutdown()
    }

    @Test fun `block adapter copies event fields before queuing and closing prevents pending dispatch`() {
        val host = Host()
        val event = mockk<BlockBreakEvent>()
        val block = mockk<Block>()
        every { event.isCancelled } returns false
        every { event.player } returns host.player
        every { event.block } returns block
        every { block.type } returns Material.DIAMOND_ORE
        val received = mutableListOf<SkillContext>()
        val trigger = BlockBreakTrigger(host.plugin)
        val handle = trigger.listen { received.add(it) }
        trigger.onEvent(event)
        assertTrue(received.isEmpty())
        every { event.block } throws AssertionError("Event retained")
        every { event.player } throws AssertionError("Event retained")
        host.flush()
        assertEquals("DIAMOND_ORE", received.single().getText("block"))
        assertSame(host.player, received.single().caster)
        every { event.player } returns host.player
        every { event.block } returns block
        trigger.onEvent(event)
        handle.close()
        handle.close()
        host.flush()
        assertEquals(1, received.size)
        verify(exactly = 1) { host.manager.registerEvents(trigger, host.plugin) }
        verify { host.handles.last().cancel() }
    }

    @Test fun `cancelled events and generated damage never queue new skills`() {
        val host = Host()
        val event = mockk<EntityDamageByEntityEvent>()
        every { event.isCancelled } returns true
        val melee = MeleeAttackTrigger(host.plugin)
        val taken = DamageTakenTrigger(host.plugin)
        val handles = listOf(melee.listen {}, taken.listen {})
        melee.onEvent(event)
        assertTrue(host.queued.isEmpty())
        every { event.isCancelled } returns false
        SkillDamageScope.during {
            melee.onEvent(event)
            taken.onEvent(event)
        }
        assertTrue(host.queued.isEmpty())
        handles.forEach { it.close() }
    }

    @Test fun `projectile hit resolves a player shooter without reading that players live state`() {
        val host = Host()
        val event = mockk<ProjectileHitEvent>()
        val projectile = mockk<Projectile>()
        val victim = mockk<LivingEntity>()
        every { event.isCancelled } returns false
        every { event.entity } returns projectile
        every { projectile.shooter } returns host.player
        every { event.hitEntity } returns victim
        every { event.hitBlock } returns null
        every { victim.type } returns EntityType.ZOMBIE
        val received = mutableListOf<SkillContext>()
        val trigger = ProjectileHitTrigger(host.plugin)
        val handle = trigger.listen { received.add(it) }
        trigger.onEvent(event)
        host.flush()
        assertSame(host.player, received.single().caster)
        assertSame(victim, received.single().victim)
        assertEquals("ZOMBIE", received.single().getText("entity"))
        handle.close()
    }

    @Test fun `sneak and sprint only dispatch starting transitions`() {
        val host = Host()
        val sneakEvent = mockk<PlayerToggleSneakEvent>()
        val sprintEvent = mockk<PlayerToggleSprintEvent>()
        every { sneakEvent.isCancelled } returns false
        every { sprintEvent.isCancelled } returns false
        every { sneakEvent.player } returns host.player
        every { sprintEvent.player } returns host.player
        every { sneakEvent.isSneaking } returns false
        every { sprintEvent.isSprinting } returns false
        val received = mutableListOf<SkillContext>()
        val sneak = SneakTrigger(host.plugin)
        val sprint = SprintTrigger(host.plugin)
        val handles = listOf(sneak.listen { received.add(it) }, sprint.listen { received.add(it) })
        sneak.onEvent(sneakEvent); sprint.onEvent(sprintEvent)
        assertTrue(host.queued.isEmpty())
        every { sneakEvent.isSneaking } returns true
        every { sprintEvent.isSprinting } returns true
        sneak.onEvent(sneakEvent); sprint.onEvent(sprintEvent)
        host.flush()
        assertEquals(2, received.size)
        handles.forEach { it.close() }
    }

    @Test fun `every built in trigger is a separate component with a unique id and listener handle`() {
        val host = Host()
        val triggers = BuiltinComponents.triggerTypes(host.plugin)
        assertEquals(12, triggers.size)
        assertEquals(12, triggers.map { it.id }.toSet().size)
        assertEquals(12, triggers.map { it.javaClass }.toSet().size)
        triggers.forEach { it.listen {}.close() }
    }

    @Test fun `melee trigger accepts direct attacks but does not mistake thorns for an attack`() {
        val host = Host()
        val event = mockk<EntityDamageByEntityEvent>()
        val victim = mockk<LivingEntity>()
        every { event.isCancelled } returns false
        every { event.damager } returns host.player
        every { event.entity } returns victim
        every { victim.type } returns EntityType.ZOMBIE
        every { event.finalDamage } returns 3.0
        every { event.cause } returns DamageCause.ENTITY_ATTACK
        val received = mutableListOf<SkillContext>()
        val trigger = MeleeAttackTrigger(host.plugin)
        val handle = trigger.listen { received.add(it) }
        trigger.onEvent(event)
        every { event.cause } returns DamageCause.THORNS
        trigger.onEvent(event)
        host.flush()
        assertEquals(1, received.size)
        assertEquals(3.0, received.single().getNumber("damage"))
        handle.close()
    }

    @Test fun `Paper cancelled death events do not emit death skills`() {
        val host = Host()
        val event = mockk<PlayerDeathEvent>()
        every { event.isCancelled } returns true
        val trigger = DeathTrigger(host.plugin)
        val handle = trigger.listen {}
        trigger.onEvent(event)
        assertTrue(host.queued.isEmpty())
        handle.close()
    }
}
