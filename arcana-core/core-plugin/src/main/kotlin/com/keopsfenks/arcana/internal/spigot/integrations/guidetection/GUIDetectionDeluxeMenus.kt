package com.keopsfenks.arcana.internal.spigot.integrations.guidetection

import com.extendedclip.deluxemenus.menu.Menu
import com.keopsfenks.arcana.core.integrations.guidetection.GUIDetectionIntegration
import org.bukkit.entity.Player

class GUIDetectionDeluxeMenus: GUIDetectionIntegration {
    override fun hasGUIOpen(player: Player): Boolean {
        return Menu.inMenu(player)
    }

    override fun getPluginName(): String {
        return "DeluxeMenus"
    }
}
