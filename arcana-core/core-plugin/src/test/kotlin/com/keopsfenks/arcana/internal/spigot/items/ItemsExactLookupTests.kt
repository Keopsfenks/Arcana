package com.keopsfenks.arcana.internal.spigot.items

import com.keopsfenks.arcana.core.Arcana
import com.keopsfenks.arcana.core.fast.FastItemStack
import com.keopsfenks.arcana.core.items.Items
import com.keopsfenks.arcana.core.items.TestableItem
import com.keopsfenks.arcana.core.items.provider.ItemProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import java.util.concurrent.atomic.AtomicInteger
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class ItemsExactLookupTests {
    companion object {
        private const val CUSTOM_DATA = "minecraft:custom_data"
        private val arcana = mockk<Arcana>(relaxed = true)
        private val nextHash = AtomicInteger()

        @JvmStatic
        @BeforeAll
        fun stubArcana() {
            mockkStatic(Arcana::class)
            every { Arcana.get() } returns arcana
            every { arcana.createNamespacedKey(any(), any()) } answers {
                NamespacedKey(firstArg<String>(), secondArg<String>())
            }
            mockkObject(Material.STONE)
            every { Material.STONE.isAir } returns false
        }

        @JvmStatic
        @AfterAll
        fun unstubArcana() {
            unmockkStatic(Arcana::class)
            unmockkObject(Material.STONE)
        }
    }

    @Test
    fun `wrapped item keeps its key`() {
        val stack = mockk<ItemStack>(relaxed = true) {
            every { hasItemMeta() } returns true
            every { amount } returns 1
        }
        val key = NamespacedKey("test", "registered_item")
        val testable = mockk<TestableItem> {
            every { shouldMarkAsCustom() } returns true
            every { matches(any()) } returns true
            every { item } returns stack
        }

        Items.registerCustomItem(key, testable)

        try {
            assertEquals("test:registered_item", Items.toExactLookupString(stack))
        } finally {
            Items.removeCustomItem(key)
        }
    }

    @Test
    fun `registered key keeps amount`() {
        val stack = stack(3)
        val key = NamespacedKey("test", "amount_item")

        Items.registerCustomItem(key, testable(stack))

        try {
            assertEquals("test:amount_item 3", Items.toExactLookupString(stack))
        } finally {
            Items.removeCustomItem(key)
        }
    }

    @Test
    fun `provider key keeps data`() {
        val stack = stack()
        every { arcana.getComponent(stack, CUSTOM_DATA) } returns mapOf("a" to "b")

        provided("exactkept", "item" to testable(stack))

        assertEquals("exactkept:item", Items.toExactLookupString(stack))
    }

    @Test
    fun `lost data falls back to snbt`() {
        val stack = stack()
        val resolved = stack()
        every { arcana.getComponent(stack, CUSTOM_DATA) } returns mapOf("a" to "b")
        every { arcana.getComponent(resolved, CUSTOM_DATA) } returns emptyMap<String, Any>()
        every { arcana.toSNBT(stack) } returns """{ id: "stone", custom_data: { a: "b c" } }"""

        provided("exactlost", "item" to testable(result = resolved, matching = stack))

        assertEquals("""{id:"stone",custom_data:{a:"b c"}}""", Items.toExactLookupString(stack))
    }

    @Test
    fun `provider ties use lowest key`() {
        val stack = stack()

        provided(
            "exactties",
            "m" to testable(stack),
            "z" to testable(stack),
            "a" to testable(stack),
            "t" to testable(stack)
        )

        assertEquals("exactties:a", Items.toExactLookupString(stack))
    }

    @Test
    fun `custom replaces provider`() {
        val stack = stack()
        val replacement = stack()
        every { arcana.getComponent(stack, CUSTOM_DATA) } returns mapOf("a" to "b")
        every { arcana.getComponent(replacement, CUSTOM_DATA) } returns emptyMap<String, Any>()
        every { arcana.toSNBT(stack) } returns "{snbt}"

        provided("exactswap", "item" to testable(result = replacement, matching = stack))
        assertEquals("{snbt}", Items.toExactLookupString(stack))

        Items.registerCustomItem(NamespacedKey("exactswap", "item"), testable(result = replacement, matching = stack))

        assertEquals("exactswap:item", Items.toExactLookupString(stack))
    }

    private fun stack(stackAmount: Int = 1): ItemStack {
        val stack = mockk<ItemStack>(relaxed = true) {
            every { hasItemMeta() } returns true
            every { amount } returns stackAmount
            every { type } returns Material.STONE
        }
        val hash = nextHash.incrementAndGet()
        every { arcana.createFastItemStack(stack) } returns object : FastItemStack by mockk(relaxed = true) {
            override fun hashCode(): Int = hash
        }
        return stack
    }

    private fun testable(result: ItemStack, matching: ItemStack = result): TestableItem = mockk {
        every { shouldMarkAsCustom() } returns true
        every { matches(any()) } answers { firstArg<ItemStack?>() === matching }
        every { item } returns result
    }

    private fun provided(namespace: String, vararg items: Pair<String, TestableItem>) {
        val byId = items.toMap()
        Items.registerItemProvider(object : ItemProvider(namespace) {
            override fun provideForKey(key: String): TestableItem? = byId[key]
        })
        items.forEach { (id, _) -> Items.lookup("$namespace:$id") }
    }
}
