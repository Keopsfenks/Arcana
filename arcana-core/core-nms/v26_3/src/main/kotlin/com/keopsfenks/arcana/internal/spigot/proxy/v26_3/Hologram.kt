package com.keopsfenks.arcana.internal.spigot.proxy.v26_3

import com.keopsfenks.arcana.core.integrations.hologram.HologramOptions
import com.keopsfenks.arcana.internal.spigot.proxies.HologramProxy
import com.keopsfenks.arcana.internal.spigot.proxies.NativeHologramHandle
import com.keopsfenks.arcana.internal.spigot.proxy.v26_3.hologram.V26_3HologramHandle
import org.bukkit.Location

class Hologram : HologramProxy {
    override fun createHandle(location: Location, options: HologramOptions): NativeHologramHandle {
        return V26_3HologramHandle.create(location, options)
    }
}
