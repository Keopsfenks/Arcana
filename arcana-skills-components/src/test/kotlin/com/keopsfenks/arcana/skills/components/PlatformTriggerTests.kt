package com.keopsfenks.arcana.skills.components

import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.triggers.*
import io.mockk.*
import org.bukkit.Location
import org.bukkit.entity.*
import org.bukkit.event.player.*
import org.bukkit.event.entity.*
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.Material
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.UUID

class PlatformTriggerTests {
    private fun NativeComponentFixture.install(trigger: SkillPlatformTrigger) = platform.register(root.plugin, SkillComponentType.TRIGGER, SkillKey("arcana", trigger.descriptor().id()), trigger)
    @Test fun `legacy registered player trigger reaches native definitions exactly once`(@TempDir dir: Path) {
        val f = NativeComponentFixture(dir)
        f.write(options = "triggers: [attack]", effects = "  - type: mark"); f.load()
        f.engine.trigger("attack", f.root.context()).toCompletableFuture().join(); assertEquals(listOf("mark"), f.marks)
        f.engine.shutdown()
    }
    @Test fun `jump held slot and equipment emit only actual transitions`(@TempDir dir: Path) {
        val f = NativeComponentFixture(dir); val jump = JumpSkillTrigger(f.root.plugin); val held = HeldSlotSkillTrigger(f.root.plugin); val equip = EquipSkillTrigger(f.root.plugin)
        val got = mutableListOf<SkillInvocation>(); val handles = listOf(jump.listen { got.add(it) }, held.listen { got.add(it) }, equip.listen { got.add(it) })
        val j = mockk<com.destroystokyo.paper.event.player.PlayerJumpEvent>(); every { j.isCancelled } returns true
        jump.onEvent(j); every { j.isCancelled } returns false; every { j.from } returns Location(f.root.world, 0.0, 64.0, 0.0); every { j.to } returns Location(f.root.world, 0.0, 65.0, 0.0); every { j.player } returns f.root.caster
        jump.onEvent(j); assertEquals(1, got.size)
        val h = mockk<PlayerItemHeldEvent>(); every { h.isCancelled } returns false; every { h.previousSlot } returns 1; every { h.newSlot } returns 1
        held.onEvent(h); assertEquals(1, got.size); every { h.newSlot } returns 2; every { h.player } returns f.root.caster; held.onEvent(h); assertEquals(2, got.size)
        val e = mockk<io.papermc.paper.event.entity.EntityEquipmentChangedEvent>(); val change = mockk<io.papermc.paper.event.entity.EntityEquipmentChangedEvent.EquipmentChange>()
        val old = mockk<ItemStack>(); val item = mockk<ItemStack>(); every { old.type } returns Material.STONE; every { old.amount } returns 1; every { item.type } returns Material.APPLE; every { item.amount } returns 2
        every { change.oldItem() } returns old; every { change.newItem() } returns old; every { e.equipmentChanges } returns mapOf(EquipmentSlot.HAND to change)
        equip.onEvent(e); assertEquals(2, got.size); every { change.newItem() } returns item; every { e.entity } returns f.root.victim
        equip.onEvent(e); assertEquals(3, got.size); assertEquals("APPLE", got.last().data()["equipment"]!!.map()["HAND"]!!.map()["material"]!!.text())
        handles.forEach { it.close() }; f.engine.shutdown()
    }
    @Test fun `damaged projectile attribution and kill use identity snapshots`(@TempDir dir: Path) {
        val f = NativeComponentFixture(dir); val damaged = EntityDamagedSkillTrigger(f.root.plugin); val kill = KillSkillTrigger(f.root.plugin); val got = mutableListOf<SkillInvocation>()
        val a = damaged.listen { got.add(it) }; val b = kill.listen { got.add(it) }
        val projectile = mockk<Projectile>(); val id = UUID.randomUUID(); every { projectile.uniqueId } returns id; every { projectile.shooter } returns f.root.caster
        val e = mockk<EntityDamageByEntityEvent>(); every { e.isCancelled } returns false; every { e.entity } returns f.root.victim; every { e.damager } returns projectile; every { e.finalDamage } returns 3.0; every { e.cause } returns EntityDamageEvent.DamageCause.PROJECTILE
        damaged.onEvent(e); assertEquals(f.root.caster.uniqueId, got.last().target().get().id()); assertEquals(id.toString(), got.last().data()["damage_source_id"]!!.text())
        val death = mockk<EntityDeathEvent>(); val source = mockk<org.bukkit.damage.DamageSource>(); every { death.damageSource } returns source; every { source.causingEntity } returns f.root.caster; every { source.directEntity } returns projectile; every { death.entity } returns f.root.victim
        f.scheduler.on(f.root.victim) { kill.onEvent(death) }.future.join(); assertEquals(2, got.size); assertEquals(f.root.caster.uniqueId, got.last().actor().id())
        a.close(); b.close(); f.engine.shutdown()
    }
    @Test fun `delayed generated native damage cannot create trigger roots and bounded guard retires`(@TempDir dir: Path) {
        val f = NativeComponentFixture(dir); val trigger = CustomSkillTrigger(); f.install(trigger)
        f.write(options = "triggers: [custom]", effects = "  - type: mark"); f.load()
        val id = UUID.randomUUID(); f.platform.generatedSources.reserve().use { it.bind(id) }
        trigger.fire(f.invocation().derive().data(mapOf("damage_source_id" to id.toString())).build()); f.scheduler.advance(1); assertTrue(f.marks.isEmpty())
        val time = java.util.concurrent.atomic.AtomicLong(); val guard = com.keopsfenks.arcana.skills.platform.events.GeneratedDamageSources(time::get, 1)
        guard.reserve().use { it.bind(id) }; assertThrows(IllegalStateException::class.java) { guard.reserve() }
        guard.retire(id); time.set(999_999_999); assertTrue(guard.contains(id)); time.incrementAndGet(); assertFalse(guard.contains(id))
        guard.reserve().close(); guard.close(); f.engine.shutdown()
    }
    @Test fun `native custom dispatch binds emission revisions and empty indexes queue nothing`(@TempDir dir: Path) {
        val f = NativeComponentFixture(dir); val trigger = CustomSkillTrigger("custom"); f.install(trigger)
        trigger.fire(f.invocation()); assertEquals(0, f.platform.triggerSubscriptions.pendingCount)
        f.write(options = "triggers: [custom]", effects = "  - type: mark\n    name: old"); f.load()
        trigger.fire(f.invocation()); assertEquals(1, f.platform.triggerSubscriptions.pendingCount)
        f.write(options = "triggers: [custom]", effects = "  - type: mark\n    name: replacement"); f.load()
        f.scheduler.advance(1); assertTrue(f.marks.isEmpty()); assertEquals(0, f.platform.triggerSubscriptions.pendingCount)
        trigger.fire(f.invocation()); f.scheduler.advance(1); assertEquals(listOf("replacement"), f.marks)
        f.engine.shutdown()
    }
    @Test fun `owner close removes queued native work and listener`(@TempDir dir: Path) {
        val f = NativeComponentFixture(dir); val trigger = CustomSkillTrigger("custom"); f.install(trigger)
        f.write(options = "triggers: [custom]", effects = "  - type: mark"); f.load()
        trigger.fire(f.invocation()); f.engine.closeOwner(f.root.plugin); f.scheduler.advance(1)
        assertTrue(f.marks.isEmpty()); assertEquals(0, f.platform.triggerSubscriptions.pendingCount)
        trigger.fire(f.invocation()); assertEquals(0, f.platform.triggerSubscriptions.pendingCount); f.engine.shutdown()
    }
    @Test fun `timer requires owned recipient and positive interval and closes all jobs`(@TempDir dir: Path) {
        val f = NativeComponentFixture(dir); f.install(TimerSkillTrigger())
        f.write(options = "triggers: [timer]", effects = "  - type: mark"); f.load()
        val actor = f.invocation().actor(); val key = SkillKey("arcana", "timer")
        assertThrows(IllegalArgumentException::class.java) { f.platform.subscribeTimer(f.root.plugin, key, actor, 0) }
        val timer = f.platform.subscribeTimer(f.root.plugin, key, actor, 1)
        f.scheduler.advance(1); f.scheduler.advance(1); assertEquals(1, f.marks.size)
        timer.close(); f.scheduler.advance(10); assertEquals(1, f.marks.size); assertEquals(0, f.platform.managed.size)
        f.engine.shutdown()
    }
    @Test fun `move skips rotations teleports and cancellations and never retains the event`(@TempDir dir: Path) {
        val f = NativeComponentFixture(dir); val t = MoveSkillTrigger(f.root.plugin); val got = mutableListOf<SkillInvocation>(); val h = t.listen { got.add(it) }
        val e = mockk<PlayerMoveEvent>(); val from = Location(f.root.world, 0.0, 64.0, 0.0); val to = from.clone()
        every { e.isCancelled } returns true; t.onEvent(e); assertTrue(got.isEmpty())
        every { e.isCancelled } returns false; every { e.player } returns f.root.caster; every { e.from } returns from; every { e.to } returns to
        to.yaw = 90f; t.onEvent(e); assertTrue(got.isEmpty()); to.x = 1.0; t.onEvent(e); assertEquals(1, got.size)
        every { e.to } throws AssertionError("event retained"); assertEquals(1.0, got.single().data()["to_x"]!!.number())
        h.close(); f.engine.shutdown()
    }
    @Test fun `entity attacks exclude players thorns and generated damage`(@TempDir dir: Path) {
        val f = NativeComponentFixture(dir); val t = EntityAttackSkillTrigger(f.root.plugin); val got = mutableListOf<SkillInvocation>(); val h = t.listen { got.add(it) }
        val e = mockk<EntityDamageByEntityEvent>(); every { e.isCancelled } returns false; every { e.cause } returns EntityDamageEvent.DamageCause.THORNS
        t.onEvent(e); assertTrue(got.isEmpty()); every { e.cause } returns EntityDamageEvent.DamageCause.ENTITY_ATTACK
        every { e.damager } returns f.root.caster; t.onEvent(e); assertTrue(got.isEmpty())
        every { e.damager } returns f.root.victim; every { e.entity } returns f.root.caster; every { e.finalDamage } returns 4.0
        t.onEvent(e); assertEquals(f.root.victim.uniqueId, got.single().actor().id())
        com.keopsfenks.arcana.skills.execution.SkillDamageScope.during { t.onEvent(e) }; assertEquals(1, got.size)
        h.close(); f.engine.shutdown()
    }
    @Test fun `consume and shoot copy hand item and projectile identities without foreign actor state`(@TempDir dir: Path) {
        val f = NativeComponentFixture(dir); val consume = ConsumeSkillTrigger(f.root.plugin); val shoot = ShootSkillTrigger(f.root.plugin)
        val got = mutableListOf<SkillInvocation>(); val a = consume.listen { got.add(it) }; val b = shoot.listen { got.add(it) }
        val item = mockk<ItemStack>(); every { item.type } returns Material.APPLE; every { item.amount } returns 1
        val e = mockk<PlayerItemConsumeEvent>(); every { e.isCancelled } returns false; every { e.player } returns f.root.caster; every { e.item } returns item; every { e.hand } returns EquipmentSlot.OFF_HAND
        consume.onEvent(e); assertEquals("OFF_HAND", got.last().data()["hand"]!!.text())
        val s = mockk<EntityShootBowEvent>(); val projectile = mockk<Projectile>(); val id = UUID.randomUUID()
        every { s.isCancelled } returns false; every { s.entity } returns f.root.victim; every { s.projectile } returns projectile; every { projectile.uniqueId } returns id; every { s.force } returns .75f; every { s.hand } returns EquipmentSlot.HAND; every { s.bow } returns null
        shoot.onEvent(s); assertEquals(id.toString(), got.last().data()["projectile_id"]!!.text()); assertEquals(f.root.victim.uniqueId, got.last().actor().id())
        every { s.isCancelled } returns true; shoot.onEvent(s); assertEquals(2, got.size); a.close(); b.close(); f.engine.shutdown()
    }
}
