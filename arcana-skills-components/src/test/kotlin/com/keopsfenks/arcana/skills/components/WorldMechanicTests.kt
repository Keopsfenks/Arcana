package com.keopsfenks.arcana.skills.components
import com.keopsfenks.arcana.skills.components.effects.world.*
import io.mockk.*
import org.bukkit.*
import org.bukkit.block.*
import org.bukkit.block.data.BlockData
import org.bukkit.event.*
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class WorldMechanicTests {
    @TempDir lateinit var directory: Path
    private val fixtures = mutableListOf<NativeComponentFixture>()
    @BeforeEach fun resolver() { mockkStatic(Bukkit::class) }
    @AfterEach fun cleanup() { fixtures.forEach { it.engine.shutdown() }; unmockkStatic(Bukkit::class) }
    private inner class Rig {
        val f = NativeComponentFixture(directory).also(fixtures::add)
        var loaded = true; var cancel = false; var changes = 0
        var observe: (Event) -> Unit = {}
        val events = mutableListOf<Event>(); val block = mockk<Block>(); val state = mockk<BlockState>()
        val stone = data(Material.STONE); val dirt = data(Material.DIRT); val air = data(Material.AIR)
        var current = dirt
        fun data(material: Material) = mockk<BlockData>().also { every { it.material } returns material; every { it.clone() } returns it }
        fun owner() { assertTrue(f.scheduler.current is Location, "World mutation requires location ownership") }
        init {
            every { Bukkit.getWorld(f.worldId) } answers { f.scheduler.assertOwner("global"); f.root.world }
            every { f.root.world.isChunkLoaded(any<Int>(), any<Int>()) } answers { owner(); loaded }
            every { f.root.world.minHeight } answers { owner(); 0 }; every { f.root.world.maxHeight } answers { owner(); 256 }
            every { f.root.world.getBlockAt(any<Int>(), any<Int>(), any<Int>()) } answers { owner(); block }
            every { block.state } answers { owner(); state }; every { block.blockData } answers { owner(); current }; every { block.type } answers { owner(); current.material }
            every { block.getRelative(BlockFace.DOWN) } answers { owner(); block }
            every { block.setBlockData(any(), any<Boolean>()) } answers { owner(); current = firstArg(); changes++; Unit }
            every { state.update(true, false) } answers { owner(); current = dirt; true }
            val held = f.root.caster.inventory.itemInMainHand; every { held.clone() } returns held
            val dispatch: (Event) -> Unit = { event -> owner(); events.add(event); observe(event); if (cancel && event is Cancellable) event.isCancelled = true }
            f.install(listOf(SetBlockMechanic(resolve = { stone }, events = dispatch), BreakBlockMechanic(air = { air }, events = dispatch), ExplosionMechanic(), LightningMechanic()))
        }
    }
    @Test fun playerBlockPlaceExposesProposedStateAndRollsBackOnNativeCancellation() {
        val r = Rig(); r.cancel = true; r.f.write(effects = "  - {type: set_block, block: stone}"); r.f.load()
        assertFalse(r.f.cast().join().isSuccess); assertSame(r.dirt, r.current); assertTrue(r.events.single() is BlockPlaceEvent)
        r.cancel = false; assertTrue(r.f.cast().join().isSuccess); assertSame(r.stone, r.current)
    }
    @Test fun mobChangesUseEntityEventsAndPlayerBreakObeysItsNativeCancellation() {
        val r = Rig(); r.f.write(effects = "  - {type: set_block, block: stone}"); r.f.write("break", effects = "  - {type: break_block}"); r.f.load()
        r.cancel = true; assertFalse(r.f.cast(mob = true).join().isSuccess); assertTrue(r.events.last() is EntityChangeBlockEvent); assertSame(r.dirt, r.current)
        assertFalse(r.f.cast("break").join().isSuccess); assertTrue(r.events.last() is BlockBreakEvent); assertEquals(0, r.changes)
        r.cancel = false; assertTrue(r.f.cast("break").join().isSuccess); assertSame(r.air, r.current)
        verify(exactly = 0) { r.block.breakNaturally() }
    }
    @Test fun unloadedChunksNeverReachEventsOrMutationsAndExplosionsDefaultToNoFireOrBlockDamage() {
        val r = Rig(); var explosion = 0
        every { r.f.root.world.createExplosion(any<Location>(), any<Float>(), any<Boolean>(), any<Boolean>()) } answers { r.owner(); assertFalse(thirdArg<Boolean>()); assertFalse(arg<Boolean>(3)); explosion++; true }
        r.f.write(effects = "  - {type: set_block, block: stone}"); r.f.write("boom", effects = "  - {type: explosion, power: 1}"); r.f.load()
        r.loaded = false; assertFalse(r.f.cast().join().isSuccess); assertEquals(0, r.changes); assertTrue(r.events.isEmpty())
        r.loaded = true; assertTrue(r.f.cast("boom").join().isSuccess); assertEquals(1, explosion)
    }
    @Test fun sourceRetirementInsideNativePlaceEventRestoresOnlyItsOwnProposedBlock() {
        val r = Rig(); r.observe = { event -> if (event is BlockPlaceEvent) { assertSame(r.stone, r.current); r.f.engine.closeOwner(r.f.root.plugin) } }
        r.f.write(effects = "  - {type: set_block, block: stone}"); r.f.load(); assertFalse(r.f.cast().join().isSuccess); assertSame(r.dirt, r.current)
    }
    @Test fun cancelledPlacementKeepsANewerBlockWrittenByAnotherListener() {
        val r = Rig(); val other = r.data(Material.GOLD_BLOCK)
        r.observe = { event -> if (event is BlockPlaceEvent) { r.current = other; event.isCancelled = true } }
        r.f.write(effects = "  - {type: set_block, block: stone}"); r.f.load(); assertFalse(r.f.cast().join().isSuccess); assertSame(other, r.current)
    }
    @Test fun nativeSkillBlockEventsDoNotReadOrReenterArcanaBlockTriggers() {
        val r = Rig(); val place = mockk<BlockPlaceEvent>(); val broken = mockk<BlockBreakEvent>()
        com.keopsfenks.arcana.skills.execution.SkillWorldMutationScope.during {
            com.keopsfenks.arcana.skills.triggers.BlockPlaceTrigger(r.f.root.plugin).onEvent(place)
            com.keopsfenks.arcana.skills.triggers.BlockBreakTrigger(r.f.root.plugin).onEvent(broken)
        }
        verify { place wasNot Called }; verify { broken wasNot Called }
        assertFalse(com.keopsfenks.arcana.skills.execution.SkillWorldMutationScope.isActive())
    }
}
