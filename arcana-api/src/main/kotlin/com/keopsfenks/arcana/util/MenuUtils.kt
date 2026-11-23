@file:JvmName("MenuUtilsExtensions")

package com.keopsfenks.arcana.util

import com.keopsfenks.arcana.core.gui.menu.Menu
import org.bukkit.entity.Player

/**
 * The arcana [Menu] this player currently has open, or null if the player has no arcana menu open.
 *
 * Inventories that are not arcana menus are not reported here.
 *
 * @see MenuUtils.getOpenMenu
 */
val Player.openMenu: Menu?
    get() = MenuUtils.getOpenMenu(this)
