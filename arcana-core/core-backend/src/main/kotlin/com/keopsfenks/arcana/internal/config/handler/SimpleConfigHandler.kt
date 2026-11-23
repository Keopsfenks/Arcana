package com.keopsfenks.arcana.internal.config.handler

import com.keopsfenks.arcana.core.config.interfaces.LoadableConfig
import com.keopsfenks.arcana.core.config.updating.ConfigHandler
import com.keopsfenks.arcana.internal.config.ArcanaLoadableConfig
import com.keopsfenks.arcana.internal.config.ArcanaUpdatableConfig

open class SimpleConfigHandler : ConfigHandler {
    private val configs = mutableListOf<LoadableConfig>()

    override fun saveAllConfigs() {
        for (config in configs) {
            config.save()
        }
    }

    override fun addConfig(config: LoadableConfig) {
        configs.add(config)
    }

    override fun updateConfigs() {
        for (config in configs) {
            when (config) {
                is ArcanaUpdatableConfig -> config.update()
                is ArcanaLoadableConfig -> config.reloadFromFile()
            }
        }
    }
}
