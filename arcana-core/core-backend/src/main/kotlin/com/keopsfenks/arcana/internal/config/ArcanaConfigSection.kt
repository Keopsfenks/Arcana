package com.keopsfenks.arcana.internal.config

import com.keopsfenks.arcana.core.config.ConfigType
import com.keopsfenks.arcana.core.placeholder.InjectablePlaceholder

class ArcanaConfigSection(
    type: ConfigType,
    values: Map<String, Any?> = emptyMap(),
    injections: Map<String, InjectablePlaceholder> = emptyMap()
) : ArcanaConfig(type) {
    init {
        this.init(values, injections)
    }
}
