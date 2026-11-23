package com.keopsfenks.arcana.internal.spigot.math

import com.keopsfenks.arcana.internal.spigot.math.token.TokenType
import com.keopsfenks.arcana.internal.spigot.math.token.Value

data class Variable(val index: Int) : Value {
    override fun getType() = TokenType.VARIABLE
    override fun getValue(variableValues: DoubleArray) = variableValues[index]
    override fun getClone() = Variable(index)
    override fun toString() = $$"$$${index + 1}"
}