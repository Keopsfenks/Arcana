package com.keopsfenks.arcana.internal.spigot.proxies

import com.keopsfenks.arcana.core.entities.ai.EntityController
import org.bukkit.entity.Mob

interface EntityControllerFactoryProxy {
    fun <T: Mob> createEntityController(
        entity: T
    ): EntityController<T>
}
