package com.keopsfenks.arcana.internal.factory

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.factory.MetadataValueFactory
import org.bukkit.metadata.FixedMetadataValue

class ArcanaMetadataValueFactory(private val plugin: ArcanaPlugin) : MetadataValueFactory {
    override fun create(value: Any): FixedMetadataValue {
        return FixedMetadataValue(plugin, value)
    }
}