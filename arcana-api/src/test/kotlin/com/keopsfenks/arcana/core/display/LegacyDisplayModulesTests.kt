package com.keopsfenks.arcana.core.display

import com.keopsfenks.arcana.core.ArcanaPlugin
import io.mockk.mockk
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LegacyDisplayModulesTests {
    private val plugin = mockk<ArcanaPlugin>(relaxed = true)

    @Test
    fun `a module overriding an old display method is legacy`() {
        val module = object : DisplayModule(plugin, 100) {
            override fun display(itemStack: ItemStack, player: Player?, vararg args: Any) {}
        }

        assertTrue(LegacyDisplayModules.overridesLegacyDisplay(module))
    }

    @Test
    fun `a module overriding only the context method is not legacy`() {
        val module = object : DisplayModule(plugin, 100) {
            override fun display(context: DisplayContext) {}
        }

        assertFalse(LegacyDisplayModules.overridesLegacyDisplay(module))
    }
}
