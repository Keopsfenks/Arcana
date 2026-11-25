package com.keopsfenks.arcana.skills.components
import com.keopsfenks.arcana.core.items.TestableItem
import com.keopsfenks.arcana.core.skills.SkillResult
import com.keopsfenks.arcana.skills.components.effects.items.*
import io.mockk.*
import org.bukkit.Material
import org.bukkit.inventory.*
import org.bukkit.inventory.meta.Damageable
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ItemMechanicTests {
    @TempDir lateinit var directory: Path
    @Test fun giveSplitsOwnedCopiesWithoutChangingTheLookupPrototype() {
        val f = NativeComponentFixture(directory); val inventory = f.root.caster.inventory
        var resolutions = 0; val amounts = mutableListOf<Int>()
        fun stack(): ItemStack = mockk<ItemStack>().also { item -> var amount = 1
            every { item.type } returns Material.DIAMOND; every { item.maxStackSize } returns 64
            every { item.amount } answers { amount }; every { item.amount = any() } answers { f.scheduler.assertOwner(f.root.caster); amount = firstArg(); Unit }
            every { item.clone() } answers { stack() }
        }
        val prototype = stack(); val lookup = mockk<TestableItem>(); every { lookup.item } returns prototype
        every { inventory.addItem(*anyVararg<ItemStack>()) } answers {
            f.scheduler.assertOwner(f.root.caster); val raw = firstArg<Any>(); val item = if (raw is Array<*>) raw[0] as ItemStack else raw as ItemStack
            amounts.add(item.amount); hashMapOf<Int, ItemStack>()
        }
        f.install(listOf(GiveItemMechanic { resolutions++; lookup })); f.write(effects = "  - {type: give_item, item: diamond, amount: 65}"); f.load()
        f.cast().join(); assertEquals(listOf(64, 1), amounts); assertEquals(1, resolutions); assertEquals(1, prototype.amount)
        verify(exactly = 0) { prototype.amount = any() }
        assertEquals(SkillResult.Status.UNSUPPORTED_ACTOR, f.cast(mob = true).join().status())
    }
    @Test fun removePreflightsAvailabilityAndChangesOnlyMatchingSlots() {
        val f = NativeComponentFixture(directory); val inventory = f.root.caster.inventory; var amount = 2; var mutations = 0
        val item = mockk<ItemStack>(); every { item.type } returns Material.DIAMOND; every { item.amount } answers { amount }
        every { item.clone() } returns item; every { item.amount = any() } answers { f.scheduler.assertOwner(f.root.caster); amount = firstArg(); Unit }
        every { inventory.storageContents } answers { f.scheduler.assertOwner(f.root.caster); arrayOf(item) }
        every { inventory.setItem(any<Int>(), any()) } answers { f.scheduler.assertOwner(f.root.caster); mutations++; Unit }
        val lookup = mockk<TestableItem>(); every { lookup.matches(any()) } answers { f.scheduler.assertOwner(f.root.caster); true }
        f.install(listOf(RemoveItemMechanic { lookup })); f.write(effects = "  - {type: remove_item, item: diamond, amount: 3}"); f.write("single", effects = "  - {type: remove_item, item: diamond, amount: 1}"); f.load()
        assertEquals(SkillResult.Status.NO_TARGET, f.cast().join().status()); assertEquals(0, mutations); assertEquals(2, amount)
        assertTrue(f.cast("single").join().isSuccess); assertEquals(1, mutations); assertEquals(1, amount)
    }
    @Test fun durabilityClampsAndEnchantsEnforceTheirResolvedNativeMaximum() {
        val f = NativeComponentFixture(directory); val inventory = f.root.caster.inventory; val item = mockk<ItemStack>(); val meta = mockk<Damageable>(); var damage = 95; var enchant = 0
        every { inventory.itemInMainHand } answers { f.scheduler.assertOwner(f.root.caster); item }
        every { inventory.setItemInMainHand(any()) } answers { f.scheduler.assertOwner(f.root.caster); Unit }
        every { item.type } returns Material.DIAMOND_SWORD; every { item.clone() } returns item; every { item.itemMeta } answers { f.scheduler.assertOwner(f.root.caster); meta }
        every { item.setItemMeta(meta) } answers { f.scheduler.assertOwner(f.root.caster); true }
        every { meta.damage } answers { f.scheduler.assertOwner(f.root.caster); damage }; every { meta.damage = any() } answers { f.scheduler.assertOwner(f.root.caster); damage = firstArg(); Unit }
        val binding = EnchantBinding(3, { true }, { _, level, _ -> f.scheduler.assertOwner(f.root.caster); enchant = level; true }, { _ -> enchant = 0; true })
        f.install(listOf(DurabilityMechanic { 100 }, EnchantMechanic { binding }))
        f.write(effects = "  - {type: durability, amount: 20}\n  - {type: enchant, enchantment: sharpness, level: 2}"); f.write("bad", effects = "  - {type: enchant, enchantment: sharpness, level: 4}"); f.load()
        assertTrue(f.cast().join().isSuccess); assertEquals(100, damage); assertEquals(2, enchant)
        assertFalse(f.cast("bad").join().isSuccess); assertEquals(2, enchant)
    }
    @Test fun missingItemsAndAmountsOutsideBoundsRejectAtTheirYamlPaths() {
        val f = NativeComponentFixture(directory); f.install(listOf(GiveItemMechanic { null }))
        for (effect in listOf("{type: give_item, item: missing}", "{type: give_item, item: diamond, amount: 4097}")) {
            f.write(effects = "  - $effect"); val report = f.engine.reload(); assertFalse(report.success()); assertTrue(report.errors().joinToString().contains("effects[0]"))
        }
    }
}
