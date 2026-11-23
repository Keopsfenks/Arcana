package com.keopsfenks.arcana.internal.spigot.proxies

import com.keopsfenks.arcana.core.integrations.hologram.HologramOptions
import org.bukkit.Location

interface HologramProxy {
    /**
     * Create a native hologram handle at [location] configured with [options].
     * The underlying entity is never added to the world.
     */
    fun createHandle(location: Location, options: HologramOptions): NativeHologramHandle
}
