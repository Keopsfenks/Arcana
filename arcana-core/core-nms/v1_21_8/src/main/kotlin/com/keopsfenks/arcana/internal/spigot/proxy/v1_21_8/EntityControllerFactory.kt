package com.keopsfenks.arcana.internal.spigot.proxy.v1_21_8

import com.keopsfenks.arcana.core.entities.ai.EntityController
import com.keopsfenks.arcana.internal.spigot.proxies.EntityControllerFactoryProxy
import com.keopsfenks.arcana.internal.spigot.proxy.v1_21_8.entity.ArcanaEntityController
import org.bukkit.entity.Mob

class EntityControllerFactory : EntityControllerFactoryProxy {
    override fun <T : Mob> createEntityController(entity: T): EntityController<T> {
        return ArcanaEntityController(entity)
    }
}
