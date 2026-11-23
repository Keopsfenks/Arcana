package com.keopsfenks.arcana.internal.spigot.integrations.shop

import com.keopsfenks.arcana.core.integrations.shop.ShopIntegration
import com.keopsfenks.arcana.core.integrations.shop.ShopSellEvent
import com.keopsfenks.arcana.core.price.impl.PriceEconomy
import dev.norska.dsw.api.DeluxeSellwandSellEvent
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class ShopDeluxeSellwands : ShopIntegration {
    override fun getSellEventAdapter(): Listener {
        return DeluxeSellwandsSellEventListeners
    }

    object DeluxeSellwandsSellEventListeners : Listener {
        @EventHandler
        fun shopEventToArcanaEvent(event: DeluxeSellwandSellEvent) {
            if (event.isCancelled) {
                return
            }

            val arcanaEvent = ShopSellEvent(event.player, PriceEconomy(event.money), null)
            Bukkit.getPluginManager().callEvent(arcanaEvent)
            event.money = arcanaEvent.value.getValue(event.player) * arcanaEvent.multiplier
        }
    }

    override fun getPluginName(): String {
        return "DeluxeSellwands"
    }
}