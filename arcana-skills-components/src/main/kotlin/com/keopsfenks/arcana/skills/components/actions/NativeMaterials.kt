package com.keopsfenks.arcana.skills.components.actions
import org.bukkit.Material

internal object NativeMaterials {
    // Material.isAir resolves the server's block registry; these immutable identities need no registry read.
    fun empty(material: Material) = material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR
}
