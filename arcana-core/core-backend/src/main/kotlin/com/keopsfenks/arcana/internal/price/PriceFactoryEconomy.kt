package com.keopsfenks.arcana.internal.price

import com.keopsfenks.arcana.core.placeholder.context.PlaceholderContext
import com.keopsfenks.arcana.core.placeholder.context.PlaceholderContextSupplier
import com.keopsfenks.arcana.core.price.Price
import com.keopsfenks.arcana.core.price.PriceFactory
import com.keopsfenks.arcana.core.price.impl.PriceEconomy

object PriceFactoryEconomy : PriceFactory {
    override fun getNames() = listOf(
        "coins",
        "$"
    )

    override fun create(baseContext: PlaceholderContext, function: PlaceholderContextSupplier<Double>): Price {
        return PriceEconomy(baseContext, function)
    }
}
