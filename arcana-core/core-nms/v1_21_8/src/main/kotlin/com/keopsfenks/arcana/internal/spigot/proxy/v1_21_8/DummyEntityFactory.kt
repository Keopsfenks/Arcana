package com.keopsfenks.arcana.internal.spigot.proxy.v1_21_8

import com.keopsfenks.arcana.internal.entities.ArcanaDummyEntity
import com.keopsfenks.arcana.internal.spigot.proxies.DummyEntityFactoryProxy
import org.bukkit.Location
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.entity.Entity
import org.bukkit.entity.Zombie

class DummyEntityFactory : DummyEntityFactoryProxy {
    override fun createDummyEntity(location: Location): Entity {
        val world = location.world as CraftWorld
        return ArcanaDummyEntity(world.createEntity(location, Zombie::class.java))
    }
}
