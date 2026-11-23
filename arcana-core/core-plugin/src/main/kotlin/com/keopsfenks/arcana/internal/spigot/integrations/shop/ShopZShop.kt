package com.keopsfenks.arcana.internal.spigot.integrations.shop

import com.keopsfenks.arcana.core.integrations.shop.ShopIntegration
import com.keopsfenks.arcana.core.integrations.shop.ShopSellEvent
import com.keopsfenks.arcana.core.price.impl.PriceEconomy
import fr.maxlego08.shop.api.events.ZShopSellEvent
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class ShopZShop : ShopIntegration {
    override fun getSellEventAdapter(): Listener {
        return ZShopSellEventListeners
    }

    object ZShopSellEventListeners : Listener {
        @EventHandler
        fun shopEventToArcanaEvent(event: ZShopSellEvent) {
            if (event.isCancelled) {
                return
            }

            val arcanaEvent = ShopSellEvent(event.player, PriceEconomy(event.price), event.button.itemStack)
            Bukkit.getPluginManager().callEvent(arcanaEvent)
            event.price = arcanaEvent.value.getValue(event.player) * arcanaEvent.multiplier
        }
    }

    override fun getPluginName(): String {
        return "zShop"
    }
}
