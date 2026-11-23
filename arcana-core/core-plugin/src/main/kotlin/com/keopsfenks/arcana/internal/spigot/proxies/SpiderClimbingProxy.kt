package com.keopsfenks.arcana.internal.spigot.proxies

import org.bukkit.entity.LivingEntity

interface SpiderClimbingProxy {
    fun disableWallPathing(entity: LivingEntity)

    fun stopClimbing(entity: LivingEntity)
}
