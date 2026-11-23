package com.keopsfenks.arcana.internal.spigot.proxies

import com.keopsfenks.arcana.core.enchant.CustomEnchantment
import com.keopsfenks.arcana.core.enchant.VanillaEnchantmentOverrides
import org.bukkit.enchantments.Enchantment

interface EnchantmentRegistryProxy {
    fun unfreeze()

    fun freeze()

    fun register(enchantment: CustomEnchantment): Enchantment

    fun unregister(enchantment: CustomEnchantment)

    fun setVanillaOverrides(overrides: VanillaEnchantmentOverrides?)

    fun getCustomEnchantment(enchantment: Enchantment): CustomEnchantment?
}
