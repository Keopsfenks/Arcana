@file:JvmName("EntityExtensions")

package com.keopsfenks.arcana.core.entities

import com.keopsfenks.arcana.core.entities.ai.EntityController
import org.bukkit.entity.Entity
import org.bukkit.entity.Mob

/** @see EntityController.getFor */
val <T : Mob> T.controller: EntityController<T>
    get() = EntityController.getFor(this)

/**
 * Check if the entity matches any [TestableEntity] in this collection.
 *
 * @see TestableEntity.matches
 */
fun Collection<TestableEntity>.matches(entity: Entity): Boolean =
    this.any { it.matches(entity) }
