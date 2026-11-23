@file:JvmName("FastItemStackExtensions")

package com.keopsfenks.arcana.core.fast

import org.bukkit.inventory.ItemStack

/** @see FastItemStack.wrap */
fun ItemStack.fast(): FastItemStack =
    FastItemStack.wrap(this)
