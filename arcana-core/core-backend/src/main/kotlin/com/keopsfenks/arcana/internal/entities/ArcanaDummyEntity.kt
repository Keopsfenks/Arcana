package com.keopsfenks.arcana.internal.entities

import com.keopsfenks.arcana.core.entities.DummyEntity
import org.bukkit.entity.Entity

class ArcanaDummyEntity(private val handle: Entity) : DummyEntity, Entity by handle {
    override fun toString(): String {
        return "DummyEntity{id=${this.entityId}}"
    }
}
