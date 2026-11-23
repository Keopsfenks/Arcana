package com.keopsfenks.arcana.internal.spigot.proxies

import com.keopsfenks.arcana.core.data.ExtendedPersistentDataContainer
import org.bukkit.persistence.PersistentDataContainer

interface ExtendedPersistentDataContainerFactoryProxy {
    fun adapt(pdc: PersistentDataContainer): ExtendedPersistentDataContainer
    fun newPdc(): PersistentDataContainer
}
