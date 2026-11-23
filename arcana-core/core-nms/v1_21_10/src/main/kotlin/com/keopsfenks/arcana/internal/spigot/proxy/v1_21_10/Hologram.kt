package com.keopsfenks.arcana.internal.spigot.proxy.v1_21_10

import com.keopsfenks.arcana.core.integrations.hologram.HologramOptions
import com.keopsfenks.arcana.internal.spigot.proxies.HologramProxy
import com.keopsfenks.arcana.internal.spigot.proxies.NativeHologramHandle
import com.keopsfenks.arcana.internal.spigot.proxy.v1_21_10.hologram.V1_21_10HologramHandle
import org.bukkit.Location

class Hologram : HologramProxy {
    override fun createHandle(location: Location, options: HologramOptions): NativeHologramHandle {
        return V1_21_10HologramHandle.create(location, options)
    }
}
