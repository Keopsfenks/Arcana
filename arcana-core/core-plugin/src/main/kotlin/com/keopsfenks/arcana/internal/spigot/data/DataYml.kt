package com.keopsfenks.arcana.internal.spigot.data

import com.keopsfenks.arcana.core.config.BaseConfig
import com.keopsfenks.arcana.core.config.ConfigType
import com.keopsfenks.arcana.internal.spigot.ArcanaSpigotPlugin

class DataYml(
    plugin: ArcanaSpigotPlugin
) : BaseConfig(
    "data",
    plugin,
    false,
    ConfigType.YAML,
    false
)
