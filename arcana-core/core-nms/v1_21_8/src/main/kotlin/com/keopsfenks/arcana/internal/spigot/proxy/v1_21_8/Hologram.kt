package com.keopsfenks.arcana.internal.spigot.proxy.v1_21_8

import com.keopsfenks.arcana.core.integrations.hologram.HologramOptions
import com.keopsfenks.arcana.internal.spigot.proxies.HologramProxy
import com.keopsfenks.arcana.internal.spigot.proxies.NativeHologramHandle
import com.keopsfenks.arcana.internal.spigot.proxy.common.hologram.CommonHologramHandle
import org.bukkit.Location

class Hologram : HologramProxy {
    override fun createHandle(location: Location, options: HologramOptions): NativeHologramHandle {
        return CommonHologramHandle.create(location, options)
    }
}
