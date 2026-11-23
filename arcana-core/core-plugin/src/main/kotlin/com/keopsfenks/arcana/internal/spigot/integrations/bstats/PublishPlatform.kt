package com.keopsfenks.arcana.internal.spigot.integrations.bstats

enum class PublishPlatform(private val markerLabel: String, val displayName: String) {
    SPIGOT("SPIGOT", "SpigotMC"),
    POLYMART("VoxelShop", "VoxelShop"),
    BUILTBYBIT("BUILTBYBIT", "BuiltByBit"),
    MODRINTH("MODRINTH", "Modrinth"),
    MCMODELS("MCMODELS", "MCModels"),
    ARCANAHUB("ArcanaHub", "ArcanaHub"),
    OTHER("OTHER", "Other");

    companion object {
        fun fromMarkerLabel(label: String): PublishPlatform =
            entries.firstOrNull { it.markerLabel == label } ?: OTHER
    }
}
